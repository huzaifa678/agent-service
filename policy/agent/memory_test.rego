package agent.memory

import rego.v1

test_allow_ordinary_persist if {
	decision.allow with input as {
		"operation": "PERSIST",
		"conversation_id": "c1",
		"content": "Your invoice INV-4821 was paid on the 3rd.",
		"attributes": {},
	}
}

test_allow_ordinary_recall if {
	decision.allow with input as {
		"operation": "RECALL",
		"conversation_id": "c1",
		"content": "The Pro plan includes 50k requests per month.",
		"attributes": {"score": 0.91},
	}
}

test_deny_persist_openai_key if {
	not decision.allow with input as {
		"operation": "PERSIST",
		"conversation_id": "c1",
		"content": "here is the key sk-abcd1234efgh5678ijkl for the integration",
		"attributes": {},
	}
}

test_deny_persist_inline_password if {
	not decision.allow with input as {
		"operation": "PERSIST",
		"conversation_id": "c1",
		"content": "log in with password=hunter2 then retry",
		"attributes": {},
	}
}

test_deny_persist_private_key if {
	not decision.allow with input as {
		"operation": "PERSIST",
		"conversation_id": "c1",
		"content": "-----BEGIN RSA PRIVATE KEY-----\nMIIE. . .",
		"attributes": {},
	}
}

test_deny_persist_when_retention_none if {
	not decision.allow with input as {
		"operation": "PERSIST",
		"conversation_id": "c1",
		"content": "a perfectly ordinary answer",
		"attributes": {"retention": "none"},
	}
}

# A secret is only screened on the way in (persist); once excluded it can never be recalled.
test_allow_recall_even_if_content_looks_secretish if {
	decision.allow with input as {
		"operation": "RECALL",
		"conversation_id": "c1",
		"content": "the password reset flow is described in the help centre",
		"attributes": {"score": 0.88},
	}
}

test_deny_recall_cross_tenant if {
	not decision.allow with input as {
		"operation": "RECALL",
		"conversation_id": "c1",
		"content": "another tenant's data",
		"attributes": {"tenant_mismatch": true},
	}
}

test_reason_is_populated_on_deny if {
	d := decision with input as {
		"operation": "PERSIST",
		"conversation_id": "c1",
		"content": "sk-abcd1234efgh5678ijkl",
		"attributes": {},
	}
	d.reason != "allowed"
	count(d.reason) > 0
}
