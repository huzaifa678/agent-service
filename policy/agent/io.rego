package agent.io

import rego.v1

# Input/output guardrail policy. Same deny-list design as agent.memory and agent.tools:
# anything no rule matches passes, so the policy is a no-op until a rule fires. INPUT rules
# screen the user's message before it reaches the model; OUTPUT rules screen the assistant's
# answer before it is returned and remembered.

content := lower(object.get(input, "content", ""))

attributes := object.get(input, "attributes", {})


injection_patterns := [
	`ignore (all )?(the )?previous instructions`,
	`disregard (the )?(above|previous|prior)`,
	`forget (everything|all previous|your instructions)`,
	`you are now (in )?(developer|dan|jailbreak)`,
	`reveal (your )?(system prompt|instructions|the prompt)`,
	`print (your )?(system prompt|instructions)`,
	`override (your )?(safety|guardrails|instructions)`,
]

deny contains "input resembles a prompt-injection / jailbreak attempt" if {
	input.direction == "INPUT"
	some pattern in injection_patterns
	regex.match(pattern, content)
}


# A credential-shaped answer must never leave the service, regardless of how it got there.
# Same shapes the memory policy screens on the way in.
secret_patterns := [
	`sk-[a-z0-9]{16,}`, # OpenAI-style API keys
	`xox[baprs]-[0-9a-z-]{10,}`, # Slack tokens
	`-----begin [a-z ]*private key-----`,
	`aws_secret_access_key`,
]

deny contains "answer contains a credential-shaped string" if {
	input.direction == "OUTPUT"
	some pattern in secret_patterns
	regex.match(pattern, content)
}


# Opt-in grounding enforcement: when the caller (or a tenant rule) sets a positive
# `min_grounding`, an answer whose retrieval confidence falls below it is withheld. Defaults
# to 0 (off), so the rule never fires unless a floor is explicitly requested — a non-RAG
# question with zero grounding is not blocked by default.
min_grounding := object.get(attributes, "min_grounding", 0)

grounding_confidence := object.get(attributes, "grounding_confidence", 0)

deny contains sprintf(
	"answer grounding %.2f is below the required floor %.2f",
	[grounding_confidence, min_grounding],
) if {
	input.direction == "OUTPUT"
	min_grounding > 0
	grounding_confidence < min_grounding
}

# The aggregate verdict handed back to the service.
decision := {
	"allow": count(deny) == 0,
	"reason": reason,
}

reason := "allowed" if count(deny) == 0

reason := concat("; ", sort([msg | some msg in deny])) if count(deny) > 0
