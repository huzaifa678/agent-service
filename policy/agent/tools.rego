package agent.tools

import rego.v1

# Tool-authorization policy. Mirrors the deny-list design of agent.memory: anything no rule
# matches is allowed, so a fresh checkout with no tenant rules is a no-op and turning the
# harness on changes nothing until a rule fires. Tools are the agent's highest-risk action
# surface (web_search reaches the internet, code_interpreter runs code), so the starter rules
# below cover the cases worth refusing out of the box.

tool := lower(object.get(input, "tool", ""))

arguments := lower(object.get(input, "arguments", ""))

attributes := object.get(input, "attributes", {})

# Optional per-tenant allow-list: when present and non-empty, a tool not on it is refused.
# Absent attribute ⇒ no allow-list ⇒ this rule never fires (deny-list default preserved).
allowed_tools := object.get(attributes, "allowed_tools", [])

deny contains sprintf("tool '%s' is not on the tenant allow-list", [tool]) if {
	count(allowed_tools) > 0
	not tool_in_allow_list
}

tool_in_allow_list if {
	some allowed in allowed_tools
	lower(allowed) == tool
}

# Explicit per-tenant denial of a specific tool.
deny contains sprintf("tool '%s' is denied for this tenant", [tool]) if {
	some denied in object.get(attributes, "denied_tools", [])
	lower(denied) == tool
}

# code_interpreter runs arbitrary code; a tenant must be explicitly entitled. Only fires when
# the entitlement flag is present and false, so it stays a no-op when the attribute is absent.
deny contains "code_interpreter requires an explicit code-execution entitlement" if {
	tool == "code_interpreter"
	object.get(attributes, "allow_code_execution", true) == false
}

# Basic SSRF guard: a web_search aimed at an internal / loopback / metadata host is refused so
# the tool cannot be steered into the cluster's private network.
ssrf_patterns := [
	`localhost`,
	`127\.0\.0\.1`,
	`169\.254\.169\.254`, # cloud instance-metadata endpoint
	`://10\.`,
	`://192\.168\.`,
	`\.internal`,
	`\.svc\.cluster\.local`,
]

deny contains "web_search target resolves to an internal or metadata host" if {
	tool == "web_search"
	some pattern in ssrf_patterns
	regex.match(pattern, arguments)
}

# The aggregate verdict handed back to the service.
decision := {
	"allow": count(deny) == 0,
	"reason": reason,
}

reason := "allowed" if count(deny) == 0

reason := concat("; ", sort([msg | some msg in deny])) if count(deny) > 0
