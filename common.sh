#!/bin/bash
mkdir -p /opt/hios/aws
function _detEnv()
{
  AWSREGION=$(curl -s ${AWS_BASE_URL}/dynamic/instance-identity/document | grep -i region | awk -F\" '{print $4}')
  AWSENV="Test"
  MNT="/opt/jboss"
  PXYENV="test"
  S3BUCK="hios-dev-backup"
  APP="HIOS-TEST1-PL-APACHE-01"
  ELBHOST="${HOST}-alb"
  HOSTDOMAIN="hiostest-aws.sbd2.com"
  FULLHOST="${HOST}.${HOSTDOMAIN}"
  FULLELBHOST="${ELBHOST}.${HOSTDOMAIN}"

  export AWSENV ENVLVL HOST ENVSFX ENVDMN MNT FULLHOST ENVPFX HOSTDOMAIN S3BUCK PXYENV ELBHOST FULLELBHOST FULLDRHOST APP 

}
function _getInstanceDetails()
{
  FOLD2GET="$1"
  export INSDTLFIL="instance_details.txt"
  [[ -z "$FOLD2GET" ]] && export INSDTLFILFULL="/opt/instance_details.txt" || export INSDTLFILFULL="${FOLD2GET}/${INSDTLFIL}"
  aws s3 cp s3://${S3BUCK}/${INSDTLFIL} ${INSDTLFILFULL}
}

function _writeInstanceDetails()
{
  #format - date,region,instance-id,IP,host,volume-id,snapshot-id-east,snapshot-id-west
  #if you need to add more, maintain the above order and then add more. Keep blank for any info not available

  ADDTNL=$1
  _getInstanceDetails
  echo "`date +%Y%m%d_%H%M%S`,${AWS_DEFAULT_REGION},${AWS_INSTANCE_ID},${AWS_IP_ADDR},${HOST}${ADDTNL}" >> "${INSDTLFILFULL}"
  aws s3 cp ${INSDTLFILFULL} s3://${S3BUCK}/ 
}

function _setAWSEnv()
{
  export AWS_DEFAULT_PROFILE="HIOS-EC2-FULL-ACCESS-ROLE"
  export AWS_IAM_URL="${AWS_BASE_URL}/meta-data/iam/security-credentials/${AWS_DEFAULT_PROFILE}"
  export AWS_ACCESS_KEY_ID=$(curl -s ${AWS_IAM_URL} | grep -i accesskeyid | awk -F\" '{print $4}')
  export AWS_SECRET_ACCESS_KEY=$(curl -s ${AWS_IAM_URL} | grep -i secretaccesskey | awk -F\" '{print $4}')
  export AWS_SESSION_TOKEN=$(curl -s ${AWS_IAM_URL} | grep -i token | awk -F\" '{print $4}')
  export AWS_DEFAULT_REGION=$(curl -s ${AWS_BASE_URL}/dynamic/instance-identity/document | grep -i region | awk -F\" '{print $4}')
  export AWS_DEFAULT_AZ=$(curl -s ${AWS_BASE_URL}/dynamic/instance-identity/document | grep -i availabilityzone | awk -F\" '{print $4}')
  export AWS_SAML_ROLE=$(curl -s ${AWS_BASE_URL}/meta-data/iam/info/ | grep -i arn | awk -F\" '{print $4}')
  export AWS_CONFIG_FILE=~/.aws/config
  export AWS_INSTANCE_ID=$(curl -s ${AWS_BASE_URL}/meta-data/instance-id)

  if [ ! -d ~/.aws ]; then
    mkdir ~/.aws
  fi

  if [ ! -s ~/.aws/config ]; then
    cat >> ~/.aws/config <<EOF
[profile ${AWS_DEFAULT_PROFILE}]
saml_role = ${AWS_SAML_ROLE}
region = ${AWS_DEFAULT_REGION}

[default]
output = table
region = ${AWS_DEFAULT_REGION}
EOF
    aws configure set s3.signature_version s3v4
  fi

}

function _setCommon()
{
  [[ -z "$HOST" ]] && export HOST="`hostname -s`" || export HOST
  export HOSTFQDN="`hostname -f`"
  export HOSTIP="`hostname -i`"
  export AWS_IP_ADDR=$(curl -s ${AWS_BASE_URL}/meta-data/local-ipv4)
  export HOSTINDNS="$(a=$(host ${AWS_IP_ADDR} | awk '{print $5}');echo ${a%%?})"
}
export AWS_BASE_URL="http://169.254.169.254/latest"

_detEnv
_setAWSEnv
_setCommon
