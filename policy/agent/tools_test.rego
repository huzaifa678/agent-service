package agent.tools

import rego.v1

test_allow_ordinary_tool_with_no_rules if {
	decision.allow with input as {
		"tool": "calculator",
		"arguments": "{\"expression\":\"2+2\"}",
		"attributes": {},
	}
}

test_allow_web_search_to_public_host if {
	decision.allow with input as {
		"tool": "web_search",
		"arguments": "https://example.com/search?q=backstage",
		"attributes": {},
	}
}

test_deny_tool_not_on_allow_list if {
	not decision.allow with input as {
		"tool": "code_interpreter",
		"arguments": "",
		"attributes": {"allowed_tools": ["web_search", "calculator"]},
	}
}

test_allow_tool_on_allow_list if {
	decision.allow with input as {
		"tool": "web_search",
		"arguments": "https://example.com",
		"attributes": {"allowed_tools": ["web_search", "calculator"]},
	}
}

test_deny_explicitly_denied_tool if {
	not decision.allow with input as {
		"tool": "code_interpreter",
		"arguments": "",
		"attributes": {"denied_tools": ["code_interpreter"]},
	}
}

test_deny_code_interpreter_without_entitlement if {
	not decision.allow with input as {
		"tool": "code_interpreter",
		"arguments": "print('hi')",
		"attributes": {"allow_code_execution": false},
	}
}

test_allow_code_interpreter_with_entitlement if {
	decision.allow with input as {
		"tool": "code_interpreter",
		"arguments": "print('hi')",
		"attributes": {"allow_code_execution": true},
	}
}

test_deny_web_search_to_localhost if {
	not decision.allow with input as {
		"tool": "web_search",
		"arguments": "http://localhost:8080/admin",
		"attributes": {},
	}
}

test_deny_web_search_to_metadata_endpoint if {
	not decision.allow with input as {
		"tool": "web_search",
		"arguments": "http://169.254.169.254/latest/meta-data/",
		"attributes": {},
	}
}

test_deny_web_search_to_cluster_internal if {
	not decision.allow with input as {
		"tool": "web_search",
		"arguments": "http://billing-service.svc.cluster.local/health",
		"attributes": {},
	}
}

test_reason_is_populated_on_deny if {
	d := decision with input as {
		"tool": "web_search",
		"arguments": "http://localhost/",
		"attributes": {},
	}
	d.reason != "allowed"
	count(d.reason) > 0
}
