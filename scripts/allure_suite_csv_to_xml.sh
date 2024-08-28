#!/bin/bash -e

# first argument is the place where the result will be stored
DEFAULT_DST=$1
# second argument is the url of the allure report test suites
LATEST_REPORT_SRC=$2
# The 3rd parameter is the name of the generated xml file.
JUNIT_XML=$3
JENKINS_USER=$4
JENKINS_PASS=$5
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
CSV_REPORT="testresults.csv"
# testcase.xml will be generate in DEFAULT_DST directory
XML_REPORT="${DEFAULT_DST}/${JUNIT_XML}"

curl -u "$JENKINS_USER:$JENKINS_PASS" -o "${DIR}/${CSV_REPORT}" "${LATEST_REPORT_SRC}"

echo '<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<testsuites>
   <testsuite>
      <testsuite>
         <testsuite>' > "${XML_REPORT}"

tail -n +2 "${DIR}/${CSV_REPORT}" | awk -F "," '{print "            <testcase classname=" $8 " name=" $9 " status=" $1 " time=" $4 "/>"}' >> "${XML_REPORT}"

echo '         </testsuite>
      </testsuite>
   </testsuite>
</testsuites>' >> "${XML_REPORT}"

echo "XML report generated in testcase.xml"
