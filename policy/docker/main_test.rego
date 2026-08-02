package main

import rego.v1

test_allow_temurin_multistage_nonroot if {
	count(deny) == 0 with input as [
		{"Cmd": "from", "Value": ["eclipse-temurin:21-jdk", "AS", "build"]},
		{"Cmd": "workdir", "Value": ["/app"]},
		{"Cmd": "copy", "Value": ["gradlew", "settings.gradle", "build.gradle", "./"]},
		{"Cmd": "run", "Value": ["chmod", "+x", "gradlew"]},
		{"Cmd": "run", "Value": ["./gradlew", ":agent-bootstrap:bootJar", "--no-daemon", "-x", "test"]},

		{"Cmd": "from", "Value": ["eclipse-temurin:21-jre-alpine", "AS", "runtime"]},
		{"Cmd": "workdir", "Value": ["/app"]},
		{"Cmd": "run", "Value": ["addgroup", "-S", "appgroup", "&&", "adduser", "-S", "appuser", "-G", "appgroup"]},
		{"Cmd": "copy", "Value": ["--from=build", "/app/agent-service/agent-bootstrap/build/libs/*.jar", "app.jar"]},
		{"Cmd": "user", "Value": ["appuser"]},
		{"Cmd": "expose", "Value": ["8083"]},
		{"Cmd": "entrypoint", "Value": ["java", "-jar", "app.jar"]},
	]
}

test_deny_latest_base if {
	count(deny) > 0 with input as [
		{"Cmd": "from", "Value": ["eclipse-temurin:latest"]},
	]
}

test_deny_untagged_base if {
	count(deny) > 0 with input as [
		{"Cmd": "from", "Value": ["eclipse-temurin"]},
	]
}

test_deny_root_user if {
	count(deny) > 0 with input as [
		{"Cmd": "from", "Value": ["eclipse-temurin:21-jre-alpine"]},
		{"Cmd": "user", "Value": ["root"]},
	]
}

test_allow_root_then_nonroot if {
	count(deny) == 0 with input as [
		{"Cmd": "from", "Value": ["eclipse-temurin:21-jre-alpine"]},
		{"Cmd": "user", "Value": ["root"]},
		{"Cmd": "user", "Value": ["appuser"]},
	]
}

test_deny_add_remote_url if {
	count(deny) > 0 with input as [
		{"Cmd": "from", "Value": ["eclipse-temurin:21-jre-alpine"]},
		{"Cmd": "add", "Value": ["https://example.com/app.jar", "/app.jar"]},
	]
}

test_warn_no_user if {
	count(deny) == 0 with input as [
		{"Cmd": "from", "Value": ["eclipse-temurin:21-jre-alpine"]},
	]

	count(warn) > 0 with input as [
		{"Cmd": "from", "Value": ["eclipse-temurin:21-jre-alpine"]},
	]
}

test_warn_no_healthcheck if {
	count(warn) > 0 with input as [
		{"Cmd": "from", "Value": ["eclipse-temurin:21-jre-alpine"]},
		{"Cmd": "user", "Value": ["appuser"]},
	]
}

test_warn_digest_not_used if {
	count(warn) > 0 with input as [
		{"Cmd": "from", "Value": ["eclipse-temurin:21-jdk"]},
	]
}

test_allow_stage_reference if {
	count(deny) == 0 with input as [
		{"Cmd": "from", "Value": ["eclipse-temurin:21-jdk", "AS", "build"]},
		{"Cmd": "from", "Value": ["build"]},
	]
}