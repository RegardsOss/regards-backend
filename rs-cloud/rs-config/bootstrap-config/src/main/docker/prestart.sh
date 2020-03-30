#!/bin/bash -e

declare -r TEMPLATE_HOME="/config-templates"
declare -r CONFIG_HOME="/config/regards"

# Check config directory
if [ ! -d "${CONFIG_HOME}" ]; then
    echo "Config home must exist! Add a bind mount pointing to ${CONFIG_HOME}!"
    exit 1
fi

# Check config directory is empty
# IF NOT, continue without changing config
if [ "$(ls -A "${CONFIG_HOME}")" ]; then 
    echo "Configuration directory ${CONFIG_HOME} not empty. Skipping copy and variable replacement."
    exit 0
fi

# Check config directory
if [ ! -d "${TEMPLATE_HOME}" ]; then
    echo "Template home must exist! Add a bind mount pointing to ${TEMPLATE_HOME}!" 
    exit 1
fi

# Copy templates into target config directory
cp -R "${TEMPLATE_HOME}/." "${CONFIG_HOME}"

# Resolve placeholders related to REGARDS env vars
for env_var in $(env | grep "^REGARDS.*=")
do
    regards_var="@@${env_var%%=*}@@" 
    regards_val=${env_var#*=}
    # printf >&2 "${regards_var}\t->\t${regards_val}\n"
    if [[ ${regards_val} == *"#"* ]]; then
        warning "Environment variable ${regards_var} contains a # which is a reserved character. Skipping replacement."
        # Indeed, hastag is used in sed clause below!
    else 
        find "${CONFIG_HOME}" -type f -name "*.properties" -exec sed -i "s#${regards_var}#${regards_val}#g" {} \;
    fi
done
