echo "Script $0 BEGIN with params - '$@' - `date '+%m/%d/%Y %H:%M:%S'`"
. /tmp/common.sh

CNTINIT=1
CNTFINAL=15
CNTSLEEP=60
VOLFLG=0
NEWVOLID=""
SNIDSTR=""
KMSSTR=""

ASGNAME="`aws autoscaling describe-auto-scaling-instances --instance-ids ${AWS_INSTANCE_ID} | grep -i autoscalinggroupname | awk -F\\" '{print $4}'`"
aws autoscaling update-auto-scaling-group --auto-scaling-group-name "${ASGNAME}" --vpc-zone-identifier "${ALLSUBNETS}"
if [ -z "$VOLID" ] || [ "$VOLID" == "NA" ]; then
  if [ ! -z "$SNID" ] && [ "$SNID" != "NA" ]; then
    if [ "$SNID" == "LATEST" ]; then
      echo "Retrieving latest snapshot from S3 - SNID=$SNID"
      _getInstanceDetails "/tmp/"
      SNID=$(grep "$HOST" $INSDTLFILFULL | tail -1 | awk -F, '{print $7}')
    fi
    echo "Using SNID=$SNID"
    SNIDSTR=" --snapshot-id $SNID "
  else
    KMSSTR=" --kms-key-id ${KMSKEYID} "
  fi
  NEWVOLID="`aws ec2 create-volume --encrypted ${SNIDSTR} ${KMSSTR} --availability-zone ${AWS_DEFAULT_AZ} --volume-type gp2 --size 50 | grep -i volumeid | awk -F\\" '{print $4}'`"
  echo "New volume created - ${NEWVOLID}"
  VOLID=${NEWVOLID}

  aws ec2 create-tags --resources "$VOLID" --tags Key=ASV,Value=${VOLTAGASV} Key=Environment,Value=${VOLTAGENV} Key=OwnerContact,Value=${VOLTAGOWN} Key=Name,Value="${VOLTAGNAM}" Key=InstanceName,Value="${VOLTAGNAM}"
fi

if [ ! -z "$VOLID" ] && [ "$VOLID" != "NA" ]; then
  sleep 120
  aws ec2 attach-volume --volume-id "$VOLID" --instance-id "${AWS_INSTANCE_ID}" --device "/dev/sdf"

  while [ $CNTINIT -le $CNTFINAL ]
    do
      aws ec2 describe-volumes --volume-ids "$VOLID" --filters Name=status,Values=in-use | grep -i '"state"[ ]*:[ ]*"attached"'
      if [ $? -eq 0 ]; then
        echo "Successfully attached $VOLID to ${AWS_INSTANCE_ID}"
        CNTINIT=$(($CNTFINAL+1))
        VOLFLG=1
      else
        ((CNTINIT=CNTINIT+1))
        sleep $CNTSLEEP
      fi
    done
else
  echo "Error creating/using-existing volume. No point moving fwd. Exiting."
  exit 1
fi

test $VOLFLG -eq 0 && echo "ERROR attaching and/or creating volume. Exiting." && exit 1

yum install -y perl python gcc libdbi-dbd-mysql perl-DBD-MySQL git subversion ftp gdb rng-tools patch sharutils dos2unix bind-utils mailx wget libstdc++ finger dump compat-libstdc++-33.i686 compat-libstdc++-33.x86_64 xfsprogs
yum -y update
if [ "X`grep ^Banner /etc/ssh/sshd_config`" != "X" ]; then sed -i'_orig' '/^#/! s/Banner/#Banner/g' /etc/ssh/sshd_config; fi
service sshd restart
cp -av /etc/cloud/templates/hosts.redhat.tmpl /etc/cloud/templates/hosts.redhat.tmpl_orig
echo -ne "${AWS_IP_ADDR} ${FULLHOST} ${HOST}\n\n" >> /etc/cloud/templates/hosts.redhat.tmpl
echo -ne "${AWS_IP_ADDR} ${FULLHOST} ${HOST}\n\n" >> /etc/hosts
sed -i'_orig' -r "s/(HOSTNAME=).*/\1${FULLHOST}/g" /etc/sysconfig/network
sed -i'_orig' -r 's/(- +set_hostname)/#\1/g' /etc/cloud/cloud.cfg
sed -i -r 's/(- +update_hostname)/#\1/g' /etc/cloud/cloud.cfg
hostname "${FULLHOST}"
export TZ='America/New_York'
ln -sf /usr/share/zoneinfo/${TZ} /etc/localtime
echo ZONE=\"${TZ}\"> /etc/sysconfig/clock
service ntpd restart
mkdir /${MNT}
echo -ne "\n/dev/xvdf /${MNT} ext4 defaults,noatime 0 2\n" >> /etc/fstab
mkswap /${MNT}/dev/extraswap
swapon /${MNT}/dev/extraswap
swapon -s
echo -ne "\n/${MNT}/dev/extraswap none swap sw 0 0\n" >> /etc/fstab
echo "Script $0 END `date '+%m/%d/%Y %H:%M:%S'`"