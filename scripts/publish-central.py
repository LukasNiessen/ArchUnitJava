"""Inspect or publish one reviewed Central deployment using environment-backed credentials."""

import argparse
import base64
import json
import os
import re
import urllib.error
import urllib.parse
import urllib.request
import uuid

BASE_URL = "https://central.sonatype.com/api/v1/publisher"


def validate_inputs(deployment_id, version):
    if str(uuid.UUID(deployment_id)) != deployment_id:
        raise ValueError("Expected a canonical deployment UUID")
    if not re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+", version):
        raise ValueError("Expected a final semantic release version")


def validate_status(status, deployment_id, version):
    """Accept only the exact component in a validated or already publishing state."""
    validate_inputs(deployment_id, version)
    if not isinstance(status, dict) or status.get("deploymentId") != deployment_id:
        raise ValueError("Central returned a different deployment ID")
    expected = f"pkg:maven/io.github.tristankruse/archunitjava@{version}"
    if status.get("purls") != [expected]:
        raise ValueError("Deployment must contain exactly the expected Maven coordinate")
    if status.get("deploymentState") not in {"VALIDATED", "PUBLISHING", "PUBLISHED"}:
        raise ValueError("Deployment has not passed validation")
    return status["deploymentState"]


class NoRedirects(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        raise RuntimeError("Central redirects are not accepted")


def request(path, token):
    req = urllib.request.Request(BASE_URL + path, method="POST",
                                 headers={"Authorization": "Bearer " + token})
    try:
        with urllib.request.build_opener(NoRedirects()).open(req, timeout=60) as response:
            data = response.read(1_048_577)
            if len(data) > 1_048_576:
                raise ValueError("Central response exceeded the size limit")
            return data
    except urllib.error.HTTPError as error:
        # Never echo headers, credentials or arbitrary server bodies into CI logs.
        raise RuntimeError(f"Central request failed with HTTP {error.code}") from None


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--deployment-id", required=True)
    parser.add_argument("--version", required=True)
    parser.add_argument("--mode", choices=("inspect", "publish"), default="inspect")
    args = parser.parse_args()
    validate_inputs(args.deployment_id, args.version)
    credentials = os.environ["MAVEN_CENTRAL_USERNAME"] + ":" + os.environ["MAVEN_CENTRAL_PASSWORD"]
    token = base64.b64encode(credentials.encode("utf-8")).decode("ascii")
    status = json.loads(request("/status?" + urllib.parse.urlencode({"id": args.deployment_id}), token))
    # Public component identities and deployment state are useful diagnostics; never log headers.
    print(json.dumps({key: status.get(key) for key in ("deploymentId", "deploymentState", "purls")})[:4096])
    state = validate_status(status, args.deployment_id, args.version)
    print(json.dumps({"deploymentId": args.deployment_id, "version": args.version, "state": state}))
    if args.mode == "publish" and state == "VALIDATED":
        request("/deployment/" + args.deployment_id, token)
        print("Publication requested for the reviewed, validated deployment.")
    elif args.mode == "publish":
        print("Publication already requested; no duplicate request sent.")


if __name__ == "__main__":
    main()
