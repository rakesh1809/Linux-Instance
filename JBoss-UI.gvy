{
	"AWSTemplateFormatVersion": "2010-09-09",
	"Description": "Creating a Linux HIOS-TEST2-AL-JBOSS-UI-01 server",
	"Mappings": {
		"EnvMap": {
			"TEST2": {
				"InstanceType": "m3.large",
				"SubnetId": "subnet-0aab8183c0f778d19",
				"AvailabilityZone": "us-east-1a",
				"HOST" : "HTEST2JUI01",
				"SnapshotIdEC2": "snap-05c7095c4c8446357",
				"SecurityGroups": [
					"sg-00889b8a1212da5e3",
					"sg-0f9eaf999d6fe55f3",
					"sg-0326fc71b84c4570b",
					"sg-03107e1b68ea7fa15"				]
			}
		}
	},
	"Parameters": {
		"Environment": {
			"Description": "Gives the environment of the instance e.g. dev ",
			"Type": "String",
			"Default": "TEST2"
		},
		"ImageId": {
			"Description": "Provides the unique ID of the Amazon Machine Image (AMI)",
			"Type": "AWS::EC2::Image::Id",
			"Default": "ami-0117f335f8ecfca25"
		},
    "IAMRole": {
            "Description": "The IAM Role to be attached to the instance",
            "Type": "String",
            "Default": "HIOS-EC2-FULL-ACCESS-ROLE"
        }
	},
	"Resources": {
		"EC2Instance": {
			"Type": "AWS::EC2::Instance",
			"Properties": {
				"ImageId": {
					"Ref": "ImageId"
				},
				"InstanceType": {
					"Fn::FindInMap": ["EnvMap", {
						"Ref": "Environment"
					}, "InstanceType"]
				},
				"PrivateIpAddress": "10.234.184.201",
        "IamInstanceProfile": {
                    "Ref": "IAMRole"
                },
				"BlockDeviceMappings": [{
					"DeviceName": "/dev/sda1",
					"Ebs": {
            "DeleteOnTermination" : "false",
            "VolumeSize": "50",
						"VolumeType": "gp2"
					}
				}],
				"SecurityGroupIds": {
					"Fn::FindInMap": ["EnvMap", {
						"Ref": "Environment"
					}, "SecurityGroups"]
				},
				"KeyName": "HIOS-TEST-2",
				"SubnetId": {
					"Fn::FindInMap": ["EnvMap", {
						"Ref": "Environment"
					}, "SubnetId"]
					},
				"UserData": {
					"Fn::Base64": {
						"Fn::Join": [
							"", [
								"#!/bin/bash -v \n",
	              "set -x \n",
	              "exec > >(tee /var/log/user-data.log|logger -t user-data ) 2>&1 \n",
	              "echo BEGIN `date '+%m/%d/%Y %H:%M:%S'`\n",
								"cd /tmp/ \n",
								"wget -d -S -np --no-proxy --no-check-certificate -nH https://github.cms.gov/raw/A9UR/HIOS-CFT-TEMPLATES/patch-1/TEST2/UI?token=AAAI1fEmQQmYNjbSbZabyS5Ye6v1dHr6ks5dCRxtwA%3D%3D -O env_build_cft_user_data.sh \n",
								"wget -d -S -np --no-proxy --no-check-certificate -nH https://github.cms.gov/raw/A9UR/HIOS-CFT-TEMPLATES/patch-1/main-common.sh?token=AAAI1bzaD8AYRyREIoodOlW2xHTLDWQUks5dCRE4wA%3D%3D -O common.sh \n",
								"chmod 774 /tmp/*.sh \n",
								"export HOST=",{"Fn::FindInMap": ["EnvMap",{"Ref": "Environment"},"HOST"]}," \n",
								". /tmp/env_build_cft_user_data.sh \n",
	              "yum update -y \n"
							]
						]
					}
				},
				"Tags": [
				{"Key": "Name","Value": "HIOS-TEST2-AL-JBOSS-UI-01"},
				{"Key": "Application","Value": "HIOS"},
				{"Key": "Business","Value": "UI"},
				{"Key": "Stack","Value": "TEST2"},
				{"Key": "Hostname","Value": {"Fn::FindInMap": ["EnvMap",{"Ref": "Environment"},"HOST"]}},
				{"Key": "OU","Value": "OU=Dev,OU=Servers,OU=HIOS,OU=FFM,OU=External,OU=CMS,DC=awscloud,DC=cms,DC=local"},
				{"Key": "AdministratorsGroup","Value": "HIOS Test Admins"},
				{"Key": "cpm backup","Value": "4HR"}
				],
				"Volumes": [{
					"VolumeId": {
						"Ref": "HIOSTEST2"
					},
					"Device": "/dev/xvdb"
				}]
			}
		},
		"HIOSTEST2": {
			"Type": "AWS::EC2::Volume",
			"Properties": {
				"AvailabilityZone": {
					"Fn::FindInMap": ["EnvMap", {
						"Ref": "Environment"
					}, "AvailabilityZone"]
				},
				"Tags": [
				{"Key": "Name","Value": "HIOS-TEST2-AL-JBOSS-UI-01"},
				{"Key": "Application","Value": "HIOS"},
				{"Key": "Business","Value": "UI"},
				{"Key": "Stack","Value": "TEST2"},
				{"Key": "Hostname","Value": {"Fn::FindInMap": ["EnvMap",{"Ref": "Environment"},"HOST"]}},
				{"Key": "OU","Value": "OU=Dev,OU=Servers,OU=HIOS,OU=FFM,OU=External,OU=CMS,DC=awscloud,DC=cms,DC=local"},
				{"Key": "AdministratorsGroup","Value": "HIOS Test Admins"},
				{"Key": "cpm backup","Value": "4HR"}
				],
				"SnapshotId": {
					"Fn::FindInMap": ["EnvMap", {
						"Ref": "Environment"
					}, "SnapshotIdEC2"]
				},
				"Size": "50",
				"VolumeType": "gp2"
			}
		}
	},
  "Outputs": {
	"TEST2StackDetails":
    {
      "Description": "HIOS-TEST2-AL-JBOSS-UI-01 Stack Details",
      "Value":
      {
        "Fn::Join":
        [
          "|",
          [
            { "Ref" : "ImageId" },
            { "Ref" : "IAMRole" },
            { "Ref" : "EC2Instance" },
            { "Fn::FindInMap": ["EnvMap",{"Ref": "Environment"},"HOST"]}
          ]
        ]
      }
    }
	}
}
