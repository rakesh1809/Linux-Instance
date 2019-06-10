
import boto3
import json
import logging
import sys,csv,argparse

# Define constants at once
FAILED_EXIT_CODE = 1

ACCESS_KEY=""
SECRET_KEY=""

# Enable the logger
logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)
ch = logging.StreamHandler()
ch.setLevel(logging.INFO)
formatter = logging.Formatter("%(asctime)s - %(levelname)s - %(message)s", datefmt='%Y-%m-%d %H:%M:%S %Z')
ch.setFormatter(formatter)
logger.addHandler(ch)

# Connect to AWS boto3 Client
def aws_connect_client(service,REGION):
    try:
        # Gaining API session
        #session = boto3.Session(aws_access_key_id=ACCESS_KEY, aws_secret_access_key=SECRET_KEY)
        session = boto3.Session()
        # Connect the resource
        conn_client = session.client(service, REGION)
    except Exception as e:
        logger.error('Could not connect to region: %s and resources: %s , Exception: %s\n' % (REGION, service, e))
        conn_client = None
    return conn_client

# Connect to AWS boto3 Resource
def aws_connect_resource(service,REGION):
    try:
        # Gaining API session
        #session = boto3.Session(aws_access_key_id=ACCESS_KEY, aws_secret_access_key=SECRET_KEY)
        session = boto3.Session()
        # Connect the resource
        conn_resource = session.resource(service, REGION)
    except Exception as e:
        logger.error('Could not connect to region: %s and resources: %s , Exception: %s\n' % (REGION, service, e))
        conn_resource = None
    return conn_resource

def open_file (filepath):
    try:
        f = file(filepath, 'wt')
    except Exception, e:
        f = None
        sys.stderr.write ('Could not open file %s. reason: %s\n' % (filepath, e))
    return f

def createEc2Inventory(filepath,REGION,VPCID):
    # opens file
    f = open_file (filepath)
    if not f:
        return False
    #Start write
    writer = csv.writer (f)
    writer.writerow (['Instance Name','Instance ID','Instance State','RootDeviceName',' PrivateIpAddress', 'PublicIpAddress', 'PrivateDnsName', 'PublicDnsName', 'InstanceType', 'InstanceVpcID', 'InstanceIamProfile', 'KeyName', 'SecurityGroupName', 'SecurityGroupId', 'LanuchTime', 'ImageId', 'VpcName', 'Subnet_Id', 'Subnet_Name', 'AvailabilityZone', 'Region'])
   # connect ec2 resouce using resource method
    ec = aws_connect_resource("ec2",REGION)
    if not ec:
        sys.stderr.write('Could not connect to region: %s. Skipping\n' % REGION)
        sys.exit(FAILED_EXIT_CODE)
    try:

        vpc = ec.Vpc(str(VPCID))
        vpc_client = aws_connect_client('ec2',REGION)
        response = vpc_client.describe_vpcs(VpcIds=[str(VPCID)])
    except Exception as e:
        logger.error("Cloud not able to describe the vpc. Exception: {}".format(e))
        sys.exit(FAILED_EXIT_CODE)

    vpc_vals=response['Vpcs'][0]['Tags']
    vpc_name = [vpc_vals.get('Value') for vpc_vals in vpc_vals if vpc_vals['Key'] == 'Name']

    for instance in vpc.instances.all():
        instance_id = instance.id
        tag_name = [tags.get('Value') for tags in instance.tags if tags['Key'] == 'Name']
        sg_groupname=[ sg.get('GroupName') for sg in instance.security_groups]
        sg_groupid = [sg.get('GroupId') for sg in instance.security_groups]
        try:
            subnet_vals=instance.subnet_id
            subnet = ec.Subnet(subnet_vals)
            subnet_tag_name = [tags.get('Value') for tags in subnet.tags if tags['Key'] == 'Name']
            subnet_tag_names = eval(str(subnet_tag_name).strip('[]'))
            #print(eval(str(tag_name).strip('[]')),instance_id,instance.state.get('Name'),instance.root_device_name,instance.private_ip_address,instance.public_ip_address,instance.private_dns_name,instance.public_dns_name,instance.instance_type,instance.vpc.id,instance.iam_instance_profile.get('Arn'),instance.key_name,sg_groupname,sg_groupid,instance.launch_time,instance.image_id,eval(str(vpc_name).strip('[]')),instance.placement.get('AvailabilityZone'),REGION)
            writer.writerow([eval(str(tag_name).strip('[]')),instance_id,instance.state.get('Name'),instance.root_device_name,instance.private_ip_address,instance.public_ip_address,instance.private_dns_name,instance.public_dns_name,instance.instance_type,instance.vpc.id,instance.iam_instance_profile.get('Arn'),instance.key_name,sg_groupname,sg_groupid,instance.launch_time,instance.image_id,eval(str(vpc_name).strip('[]')),subnet_vals,subnet_tag_names,instance.placement.get('AvailabilityZone'),REGION])
        except Exception as e:
            logger.error("Cloud not able to generate ec2 inventory. Exception: {}".format(e))
            sys.exit(FAILED_EXIT_CODE)
    logger.info("The Ec2 inventory has been generated successfully: {}".format(filepath))
    f.close()

def createEbsInventory(filepath,REGION,VPCID):
    # opens file
    f = open_file (filepath)
    if not f:
        return False
    #Start write
    writer = csv.writer (f)
    writer.writerow(['InstanceName', 'VolumeId', 'Encrypted', 'VolumeSize', 'VolumeState', 'VolumeIops', 'SnapshotId', 'AvailabilityZone', 'Region'])
    try:
        ec = aws_connect_resource("ec2", REGION)
        vpc = ec.Vpc(str(VPCID))
    except Exception as e:
        logger.error("Cloud not able to get the vpc list. Exception: {}".format(e))
        sys.exit(FAILED_EXIT_CODE)

    for instance in vpc.instances.all():
        instance = ec.Instance(str(instance.id))
        volumes = instance.volumes.all()
        for v in volumes:
            try:
                #print(instance.id,v.id,v.encrypted,v.size,v.state,v.iops,v.snapshot_id,v.availability_zone,REGION)
                writer.writerow([instance.id,v.id,v.encrypted,v.size,v.state,v.iops,v.snapshot_id,v.availability_zone,REGION])
            except Exception as e:
                logger.error("Cloud not able to generate the ebs volume report. Exception: {}".format(e))
                sys.exit(FAILED_EXIT_CODE)
    logger.info("The EBS Volume invetory has been generated successfully : {}".format(filepath))

    f.close()


def createSGInventory(filepath,REGION,VPCID):
    # opens file
    f = open_file (filepath)
    if not f:
        return False
    #Start write
    writer = csv.writer (f)
    writer.writerow (['SecurityGroupName', 'SecurityGroupId', 'VpcId', 'IpPermissions', 'IpProtocol', 'FromPort', 'ToPort', 'CidrIp', 'GroupId', 'Description'])
    try:
        vpc_client = aws_connect_client('ec2', REGION)
        vpc_filter = [{
            'Name': 'vpc-id',
            'Values': [VPCID]
        }]
        security_groups = vpc_client.describe_security_groups(Filters=vpc_filter)
    except Exception as e:
        logger.error("Cloud not able to describe the security group. Exception : {}".format(e))
        sys.exit(FAILED_EXIT_CODE)

    for group in security_groups['SecurityGroups']:
        group_dict = dict()
        group_dict['id'] = group['GroupId']
        group_dict['name'] = group['GroupName']
        group_dict['IpPermissions'] = "Ingress"

        for rule in group.get('IpPermissions', None):
            rule_dict = dict()
            rule_dict['ip_protocol'] = rule['IpProtocol']
            rule_dict['from_port'] = rule.get('FromPort', -2)
            rule_dict['to_port'] = rule.get('ToPort', -2)

            rule_dict['grants'] = list()

            for grant in (rule.get('IpRanges')
                          + rule.get('Ipv6Ranges')
                          + rule.get('UserIdGroupPairs')):
                grant_dict = dict()
                grant_dict['description'] = grant.get('Description', None)
                if grant.get('GroupId', None):
                    grant_dict['group_id'] = grant.get('GroupId', None)
                else:
                    grant_dict['group_id'] = None
                if 'CidrIp' in grant.keys():
                    grant_dict['cidr_ip'] = grant.get('CidrIp')
                elif 'CidrIpv6' in grant.keys():
                    grant_dict['cidr_ip'] = grant.get('CidrIpv6')
                else:
                    grant_dict['cidr_ip'] = None
                try:
                    #print group_dict['name'],group_dict['id'],VPCID,group_dict['IpPermissions'],rule_dict['ip_protocol'],rule_dict['from_port'],rule_dict['to_port'], grant_dict['cidr_ip'], grant_dict['group_id'], grant_dict['description']
                    writer.writerow([group_dict['name'],group_dict['id'],VPCID,group_dict['IpPermissions'],rule_dict['ip_protocol'],rule_dict['from_port'],rule_dict['to_port'], grant_dict['cidr_ip'], grant_dict['group_id'], grant_dict['description']])
                except Exception as e:
                    logger.error("Cloud not able to generate the security group inventory. Exception: {}".format(e))
    logger.info("The Security Group Inventory has been generated successfully : {}".format(filepath))
    f.close()

if __name__ == '__main__':

    parser = argparse.ArgumentParser(description='aws inventory script')
    parser.add_argument('-r', '--region', help='Specify the region', required=True)
    parser.add_argument('-i', '--inventory', help='Specify the inventory resources like ec2 or ebs or sg', required=True)
    parser.add_argument('-f', '--filepath', help='Specify the csv file path and name ', required=True)
    parser.add_argument('-vpc', '--vpcid', help='Specify the vpc id', required=True)

    args = vars(parser.parse_args())
    if args['inventory'] == 'ec2':
        createEc2Inventory(args['filepath'],args['region'],args['vpcid'])
    elif args['inventory'] == 'ebs':
        createEbsInventory(args['filepath'],args['region'],args['vpcid'])
    elif args['inventory'] == 'sg':
        createSGInventory(args['filepath'],args['region'],args['vpcid'])
    else:
        logger.error("Unknown Option")
        sys.exit(FAILED_EXIT_CODE)

