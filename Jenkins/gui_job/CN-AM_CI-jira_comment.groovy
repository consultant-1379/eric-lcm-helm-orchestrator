/*
Add Gerrit trigger from the repository where the merge comment should be placed
 JIRA_USER and JIRA_PASS from "CNAM FUNC USER"
 */

set +e

curl_data()
        {
            cat <<EOF
                    {
                        "body": "Patch has merged in $GERRIT_PROJECT\ncommit: [$PATCHSET_REVISION|https://gerrit-gamma.gic.ericsson.se/plugins/gitiles/$GERRIT_PROJECT/+/$GERRIT_PATCHSET_REVISION]\nbranch: $GERRIT_BRANCH\nauthor: $GERRIT_CHANGE_OWNER_NAME\nurl: $GERRIT_CHANGE_URL"
                    }
            EOF
        }


[[ $GERRIT_CHANGE_SUBJECT =~ IDUN-[0-9]* ]]
ISSUE=${BASH_REMATCH[0]}

if [ -n "$ISSUE" ]
then
PATCHSET_REVISION=`echo ${GERRIT_PATCHSET_REVISION}| cut -b -8`
curl -D- -u $JIRA_USER:$JIRA_PASS -X POST -H "Content-Type: application/json" \
-d "$(curl_data)" https://jira-oss.seli.wh.rnd.internal.ericsson.com/rest/api/2/issue/$ISSUE/comment
else
echo "Commit without Jira ticket - ignore"
exit 0
fi