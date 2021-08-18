FROM alpine:latest
ENV REGARDS_HOME /regards
# Docker volume for clients
ENV REGARDS_CLIENTS /regards-clients

# Store AMQP client JAR
ADD amqp-regards-client-*.jar ${REGARDS_HOME}/bin/
# Add default configuration
ADD application.properties ${REGARDS_HOME}/config/
# Add shell script
ADD regards-publish.sh ${REGARDS_HOME}/script/

# Testing purpose : CMD [ "/bin/bash" ]
# first lets create the directory hierarchy if needed (mkdir -p) 
CMD mkdir -p ${REGARDS_CLIENTS}/amqp/bin && \
	mkdir ${REGARDS_CLIENTS}/amqp/config && \
	mkdir ${REGARDS_CLIENTS}/amqp/data && \
	# Remove existing bin
	rm -f ${REGARDS_CLIENTS}/amqp/bin/*.jar && \
	# Copy JAR
	cp ${REGARDS_HOME}/bin/*.jar ${REGARDS_CLIENTS}/amqp/bin/ && \
	cp ${REGARDS_HOME}/config/application.properties ${REGARDS_CLIENTS}/amqp/config/application-ref.properties && \
	cp ${REGARDS_HOME}/script/*.sh ${REGARDS_CLIENTS}/amqp/