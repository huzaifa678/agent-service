package agent.memory

import rego.v1

# Case-insensitive view of the content for pattern matching.
content := lower(object.get(input, "content", ""))

attributes := object.get(input, "attributes", {})

# Anything that looks like a live credential must never enter long-term memory: it would be
# embedded, recalled into later prompts, and leaked back to the model turn after turn.
secret_patterns := [
	`sk-[a-z0-9]{16,}`, # OpenAI-style API keys
	`xox[baprs]-[0-9a-z-]{10,}`, # Slack tokens
	`aws_secret_access_key`,
	`-----begin [a-z ]*private key-----`,
	`password\s*[:=]\s*\S`,
]

deny contains "content resembles a credential or secret; not eligible for long-term memory" if {
	input.operation == "PERSIST"
	some pattern in secret_patterns
	regex.match(pattern, content)
}

# A tenant can opt out of memory retention entirely (GDPR erasure, contractual no-retention).
deny contains "tenant retention policy is 'none'; memory persistence is disabled" if {
	input.operation == "PERSIST"
	attributes.retention == "none"
}

# Never recall a passage the retrieval layer flagged as belonging to another tenant.
deny contains "passage belongs to a different tenant; not recallable" if {
	input.operation == "RECALL"
	attributes.tenant_mismatch == true
}

# The aggregate verdict handed back to the service.
decision := {
	"allow": count(deny) == 0,
	"reason": reason,
}

reason := "allowed" if count(deny) == 0

reason := concat("; ", sort([msg | some msg in deny])) if count(deny) > 0
