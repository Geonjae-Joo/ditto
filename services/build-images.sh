#!/bin/bash

# Copyright (c) 2020 Contributors to the Eclipse Foundation
#
# See the NOTICE file(s) distributed with this work for additional
# information regarding copyright ownership.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License 2.0 which is available at
# http://www.eclipse.org/legal/epl-2.0
#
# SPDX-License-Identifier: EPL-2.0

################################################################################
# This script builds Docker images consisting of the "fat-jars" of the         #
# specified services (SERVICES).                                               #
################################################################################

# Array of the services to build Docker images for.
# The pattern is MODULE_NAME:IMAGE_NAME as both can differ from each other.
SERVICES=(
  "concierge:concierge"
  "gateway:gateway"
  "policies:policies"
  "things:things"
  "thingsearch:things-search"
  "connectivity:connectivity"
)
HTTP_PROXY_LOCAL=$HTTP_PROXY
HTTPS_PROXY_LOCAL=$HTTPS_PROXY
DOCKER_BIN=docker
DOCKERFILE="dockerfile-snapshot"
DOCKER_BUILD_CONTEXT="."
SERVICE_VERSION="0-SNAPSHOT"

print_usage() {
  printf "%s [-p HTTP(S) PROXY HOST:PORT]\n" "$1"
}

print_used_proxies() {
  printf "Using HTTP_PROXY=%s\n" "$HTTP_PROXY_LOCAL"
  printf "Using HTTPS_PROXY=%s\n" "$HTTPS_PROXY_LOCAL"
}

build_docker_image() {
  module_name_base=$(echo "$1" | awk -F ":" '{ print $1 }')
  module_name=$(printf "ditto-services-%s" "$module_name_base")
  image_tag=$(printf "eclipse/ditto-%s" "$(echo "$1" | awk -F ":" '{ print $2 }')")
  printf "\nBuilding Docker image <%s> for service module <%s>\n" \
    "$image_tag" \
    "$module_name"

    $DOCKER_BIN build --pull -f $DOCKERFILE \
      --build-arg HTTP_PROXY="$HTTP_PROXY_LOCAL" \
      --build-arg HTTPS_PROXY="$HTTPS_PROXY_LOCAL" \
      --build-arg TARGET_DIR="$module_name_base"/starter/target \
      --build-arg SERVICE_STARTER="$module_name"-starter \
      --build-arg SERVICE_VERSION=$SERVICE_VERSION \
      -t "$image_tag":$SERVICE_VERSION \
      $DOCKER_BUILD_CONTEXT
}

build_all_docker_images() {
  for i in "${SERVICES[@]}"; do
    build_docker_image "$i"
  done
}

set_proxies() {
  HTTP_PROXY_LOCAL="http://$1"
  printf "Set HTTP_PROXY to %s\n" "$HTTP_PROXY_LOCAL"
  HTTPS_PROXY_LOCAL="https://$1"
  printf "Set HTTPS_PROXY to %s\n" "$HTTPS_PROXY_LOCAL"
}

evaluate_script_arguments() {
  while getopts "p:h" opt; do
    case ${opt} in
    p)
      set_proxies "$OPTARG"
      return 0
      ;;
    h | *)
      print_usage "$0"
      return 1
      ;;
    esac
  done
}

# Here the programme begins
if [ 0 -eq $# ]; then
  print_used_proxies
  build_all_docker_images
elif evaluate_script_arguments "$@"; then
  build_all_docker_images
fi
