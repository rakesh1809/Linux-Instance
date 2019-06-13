{
	"AWSTemplateFormatVersion": "2010-09-09",
	"Description": "Creating a windows batch server for dotnet application in dev env",
	"Outputs": {},
	"Mappings": {
		"EnvMap": {
			"test1": {
				"InstanceType": "m3.large",
				"SubnetId": "subnet-022cb1f25383648f2",
				"AvailabilityZone": "us-east-1a",
				"SnapshotIdEC2": "snap-0c72e4e18acf04de1",
				"SecurityGroups": [
					"sg-0829ae739fc3f3463",
					"sg-09260ecb1d6da3457"
				]
			}
		}
	},
	"Parameters": {
		"Environment": {
			"Description": "Gives the environment of the instance test1",
			"Type": "String",
			"Default": "test1"
		},
		"IAMRole": {
			"Description": "The IAM Role to be attached to the instance",
			"Type": "String",
			"Default": "HIOS-EC2-FULL-ACCESS-ROLE"
		},
		"ImageId": {
			"Description": "Provides the unique ID of the Amazon Machine Image (AMI)",
			"Type": "AWS::EC2::Image::Id",
			"Default": "ami-057af63ac7ba849a9"
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
					"Fn::FindInMap": [
						"EnvMap",
						{
							"Ref": "Environment"
						},
						"InstanceType"
					]
				},
				"IamInstanceProfile": {
					"Ref": "IAMRole"
				},
				"BlockDeviceMappings": [{
					"DeviceName": "/dev/sda1",
					"Ebs": {
						"VolumeSize": "100",
						"VolumeType": "gp2"
					}
				}],
				"SecurityGroupIds": {
					"Fn::FindInMap": [
						"EnvMap",
						{
							"Ref": "Environment"
						},
						"SecurityGroups"
					]
				},
				"KeyName": "HIOS-TEST-1",
				"SubnetId": "subnet-022cb1f25383648f2",
				"UserData": {
					"Fn::Base64": {
						"Fn::Join": [
							"",
							[
								"<powershell>\n",
								"Initialize-Disk -Number 1\n",
								"Set-Disk -Number 1 -IsOffline $False\n",
"Set-Disk -Number 1 -IsReadonly $False\n",
 							 "</powershell>"
							]
						]
					}
				},
				"Tags": [{
						"Key": "Name",
						"Value": "HIOS-SAMPLE"
					},
					{
						"Key": "application",
						"Value": "hios"
					},
					{
						"Key": "business",
						"Value": "BATCH"
					},
					{
						"Key": "stack",
						"Value": "TEST1"
					},
					{
						"Key": "cpm backup",
						"Value": "4HR"
					},
					{
						"Key": "HOSTNAME",
						"Value": "HIOST2DEV"
					},
					{
						"Key": "OU",
						"Value": "OU=Dev,OU=Servers,OU=HIOS,OU=FFM,OU=External,OU=CMS,DC=awscloud,DC=cms,DC=local"
					},
					{
						"Key": "AdministratorsGroup",
						"Value": "Administrators"

					},
					{
						"Key": "AdministratorsGroup",
						"Value": "HIOS Dev Admins"

					}

				],
				"Volumes": [{
					"VolumeId": {
						"Ref": "HIOSDEV"
					},
					"Device": "/dev/xvdf"
				}]
			}
		},
		"HIOSDEV": {
			"Type": "AWS::EC2::Volume",
			"Properties": {
				"AvailabilityZone": {
					"Fn::FindInMap": [
						"EnvMap",
						{
							"Ref": "Environment"
						},
						"AvailabilityZone"
					]
				},
				"Tags": [{
						"Key": "Name",
						"Value": "HIOS-SAMPLE-D-Drive"
					},
					{
						"Key": "application",
						"Value": "hios"
					},
					{
						"Key": "business",
						"Value": "BATCH"
					},
					{
						"Key": "stack",
						"Value": "DEV"
					},
					{
						"Key": "cpm backup",
						"Value": "4HR"
					}
				],
				"SnapshotId": {
					"Fn::FindInMap": [
						"EnvMap",
						{
							"Ref": "Environment"
						},
						"SnapshotIdEC2"
					]
				},
				"Size": "100",
				"VolumeType": "gp2"
			}
		}
	}
	}
