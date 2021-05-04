#!/bin/bash -e

# usage #######################################################################
function usage
{
  declare -r USAGE_PGR_NAME="$1"
  printf >&2 "Usage : ${USAGE_PGR_NAME} -p profile [-h]\n"
  printf >&2 "\t-p profile : with profile corresponding to file application-{profile}.properties to load inside config directory.\n"
  printf >&2 "\t-h (Optional) : Display this help.\n"

  exit 1
}


# end #########################################################################
function end
{
  cd "${DIR}"
}

# error #######################################################################
function error
{
  printf >&2 "[\033[31mFAILURE\033[m]\t$1\n"
  exit 1
}

# warning ######################################################################
function warning
{
  printf >&2 "[\033[33mWARNING\033[m]\t$1\n"
}

# success #######################################################################
function success
{
  printf >&2 "[\033[32mSUCCESS\033[m]\t$1\n"
}

# main ########################################################################
declare -r DIR="$(pwd)"

declare -r PROCESSUS_NAME=$(basename $0)
declare -r PROCESSUS_DIR=$(dirname $0)

declare -r CONFIG_DIR="${PROCESSUS_DIR}/config"

declare profile

while getopts "p:h" opt
do
  case ${opt} in
    p)
        profile="${OPTARG}"
        ;;
    \?|h)
        usage "${PROCESSUS_NAME}"
        ;;
  esac
done

shift $((${OPTIND} - 1))

trap end EXIT

# Check parameters
if [[ -z "${profile}" ]]; then
    error "Missing SPRING profile related to loaded file application-{profile}.properties" 
fi

declare -r SPRING_PROFILE="${CONFIG_DIR}/application-${profile}.properties"
if [ ! -f "${SPRING_PROFILE}" ]; then
    error "Unknown profile file ${SPRING_PROFILE}"
fi

# Publish
java -jar bin/amqp-regards-client-*-spring-boot.jar --spring.config.location=file:${CONFIG_DIR}/ --spring.profiles.active=${profile}
