"""Generate docs/infrastructure/cd_s3_ssm.png"""
from pathlib import Path
from diagrams import Cluster, Diagram, Edge
from diagrams.aws.compute import EC2
from diagrams.aws.storage import S3
from diagrams.aws.management import SystemsManager
from diagrams.aws.security import IAM
from diagrams.onprem.ci import GithubActions
from diagrams.onprem.vcs import Github
from diagrams.onprem.client import User

OUT = Path(__file__).resolve().parent / "cd_s3_ssm"

with Diagram(
    "KOBE CD - GitHub Actions to EC2 via S3 and SSM",
    filename=str(OUT),
    show=False,
    direction="LR",
    graph_attr={"fontsize": "16", "bgcolor": "white", "pad": "0.5", "splines": "spline"},
    outformat="png",
):
    dev = User("Developer")
    gh = Github("GitHub\ndevelop")
    actions = GithubActions("GitHub Actions\nbuild arm64")

    with Cluster("AWS ap-northeast-1"):
        oidc = IAM("OIDC\ntemporary creds")
        s3 = S3("S3\ndeploy artifacts")
        ssm = SystemsManager("SSM\nRun Command")
        with Cluster("EC2 t4g.small"):
            ec2 = EC2("Spring Boot\ndocker load\n& restart")

    dev >> Edge(label="push / merge") >> gh >> actions
    actions >> Edge(label="assume role") >> oidc
    actions >> Edge(label="upload tar") >> s3
    actions >> Edge(label="SendCommand") >> ssm
    ssm >> Edge(label="download & deploy") >> ec2
    s3 >> Edge(style="dashed", label="GetObject") >> ec2
