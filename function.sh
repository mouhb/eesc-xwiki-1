#!/bin/bash

# Errors code
ERR_UNKNOWN_ERROR=1
ERR_UNKNOWN_ARG=2
ERR_CONF=3
ERR_UNKNOWN_SERVICE=4
ERR_TOMCAT=5
ERR_BUILD=6
ERR_XWIKI_IS_ON=7

# Possible log levels: error, warning, info, debug
function mylog() {
	LOCAL_LOG_SCRIPT="$0"
	if [ $# -eq 1 ]
	then
		LOCAL_LOG_LEVEL="info"
	else
		LOCAL_LOG_LEVEL="$1"
	fi
	LOCAL_LOG_MESSAGE="$2"
	if [ $LOCAL_LOG_LEVEL = "debug" -a "$LOCAL_LOG_MESSAGE" = "$LOG_FILE" ]
	then
		echo "XXX LOCAL_LOG_MESSAGE<$LOCAL_LOG_MESSAGE> LOG_FILE<$LOG_FILE>"
		LOCAL_LOG_MESSAGE="\n$( cat $LOG_FILE | sed 's/^/  > /g' )"
	fi
	if [ $LOCAL_LOG_LEVEL = "error" -a $# -eq 3 ]
	then
		LOCAL_LOG_ERROR_CODE=$3
	else
		LOCAL_LOG_ERROR_CODE=$ERR_UNKOWN_ERROR
	fi
	case $LOCAL_LOG_LEVEL in
		"error") LOCAL_LOG_VALUE=1 ;;
		"warning") LOCAL_LOG_VALUE=2 ;;
		"info") LOCAL_LOG_VALUE=3 ;;
		"debug") LOCAL_LOG_VALUE=4 ;;
	esac
	case $LOG_LEVEL in
		"error") LOG_VALUE=1 ;;
		"warning") LOG_VALUE=2 ;;
		"info") LOG_VALUE=3 ;;
		"debug") LOG_VALUE=4 ;;
	esac
	if [ $LOCAL_LOG_VALUE -le $LOG_VALUE ]
	then
		echo -e "$LOCAL_LOG_SCRIPT: $( echo "$LOCAL_LOG_LEVEL" | tr '[:lower:]' '[:upper:]' ): $LOCAL_LOG_MESSAGE"
	fi
	if [ $LOCAL_LOG_LEVEL = "error" ]
	then
		exit $LOCAL_LOG_ERROR_CODE
	fi
}

function log_info() {
	mylog "info" "$1"
}
function log_warning() {
	mylog "warning" "$1"
}
function log_error() {
	mylog "error" "$1" $2
}
function log_debug() {
	mylog "debug" "$1"
}
function to_log() {
    DEBUG_HEAD=$( log_debug "" )
	sed "s/^/$DEBUG_HEAD/g"
	if [ $LOG_LEVEL = "debug" ]
	then
		tee --append $LOG_FILE
	else
		tee --append $LOG_FILE > /dev/null 2>&1
	fi
}

function parse_configuration_file() {
	if [ $# -eq 1 -a -f "$1" ]
	then
		source "$1"
	else
		log_error "Cannot read the configuration file" $ERR_CONF
	fi
}

function start_xwiki() {
	curl http://localhost:8080/xwiki/bin/view/Main/WebHome > /dev/null 2>&1
	ERROR_CODE=$?
	if [ $ERROR_CODE -eq 0 ]
	then
		log_error "XWiki is already running" $ERR_XWIKI_IS_ON
	fi
	log_info "Starting XWiki"
	if [ $# -eq 1 ]
	then
		if [ $1 -eq 1 ]
		then
			cp $XWIKI_WEBXML_CAS_FILE $XWIKI_WEBXML_FILE
		else
			cp $XWIKI_WEBXML_NOCAS_FILE $XWIKI_WEBXML_FILE
		fi
	fi
	sh $XWIKI_START 2>&1 | to_log
	ERROR_CODE=1
	while [ $ERROR_CODE -ne 0 ]
	do
		log_info "Trying to connect to the server"
		sleep 1
		curl http://localhost:8080/xwiki/bin/view/Main/WebHome > /dev/null 2>&1
		ERROR_CODE=$?
	done
	log_info "XWiki is started"
}

function stop_xwiki() {
	log_info "Stopping XWiki"
	sh $XWIKI_STOP 2>&1 | to_log
	ERROR_CODE=0
	while [ $ERROR_CODE -eq 0 ]
	do
		log_info "Checking if XWiki is stopped"
		sleep 1
		ps aux | grep "java" | grep "$TO" > /dev/null 2>&1
		ERROR_CODE=$?
	done
	log_info "XWiki is stopped"
}

function restart_xwiki() {
	stop_xwiki
	start_xwiki $1
}
