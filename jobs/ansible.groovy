def buildGitUrl(projectName, upstreamCollectionName = "") {
  return 'https://github.com/ansible-middleware/' + (upstreamCollectionName ?: projectName) + '.git'
}

def upstreamCIJob(projectName, projectUpstreamName, moleculeBuildId, scenarioName = "--all") {
  new ansible.MoleculeBuilder(
        projectName: projectName,
        projectUpstreamName: projectUpstreamName,
        moleculeBuildId: moleculeBuildId,
        scenarioName: scenarioName,
        gitUrl: buildGitUrl(projectName),
        jobPrefix: "ansible-ci-",
        pathToScript: 'ansible/molecule/molecule.sh',
        podmanImage: 'localhost/molecule-runner-9',
    ).build(this)
}

def moleculeJobWithGitUrl(projectName, moleculeBuildId, gitUrl, branch = "main", scenarioName = "--all") {
  new ansible.MoleculeBuilder(
        projectName: projectName,
        moleculeBuildId: moleculeBuildId,
        scenarioName: scenarioName,
        gitUrl: gitUrl,
        branch: branch,
        jobPrefix: "ansible-ci-",
        pathToScript: 'ansible/molecule/molecule.sh',
        podmanImage: 'localhost/molecule-runner-9',
    ).build(this)
}

def downstreamCIJob(projectName, moleculeBuildId, scenarioName = "--all", projectUpstreamName = projectName) {
  new ansible.MoleculeBuilder(
        projectName: projectName,
        projectUpstreamName: projectUpstreamName,
        moleculeBuildId: moleculeBuildId,
        scenarioName: scenarioName,
        gitUrl: buildGitUrl(projectName),
        jobPrefix: "ansible-downstream-ci-",
        podmanImage: 'localhost/molecule-runner-9',
        pathToScript: 'ansible/molecule/molecule-downstream.sh',
        copyFromParentJob: "True",
        checkoutProject: "False"
    ).build(this)
}

def janusDefaultPlaybook(projectName) {
  return "playbooks/" + projectName + ".yml"
}

def janusJob(Map args, projectName) {
  new ansible.JanusBuilder(
        projectName: projectName,
        projectUpstreamName: args.projectUpstreamName ?: projectName,
        upstreamCollectionName: args.upstreamCollectionName ?: '',
        playbook: args.playbook ?: janusDefaultPlaybook(projectName),
        gitUrl: args.gitUrl ?: buildGitUrl(args.projectUpstreamName ?: projectName, args.upstreamCollectionName ?: ''),
        jobPrefix: 'ansible-janus-',
        pathToScript: 'ansible/janus.sh',
        podmanImage: 'localhost/janus',
        checkoutProject: "False",
        setupTrigger: args.setupTrigger ?: true
    ).build(this)
}


def dotJob(projectName, dotJobsPrefix, portOffset) {
   String dotProjectName = projectName + "-dot"
   new ansible.MoleculeBuilder(
     projectName: dotProjectName,
     projectUpstreamName: projectName,
     jobPrefix: dotJobsPrefix,
     moleculeBuildId: portOffset,
     checkoutProject: "True",
     gitUrl: "git@gitlab:ansible-middleware/" + dotProjectName + ".git",
     copyFromParentJob: "True",
     pathToScript: 'ansible/molecule/molecule-downstream.sh',
     podmanImage: 'localhost/molecule-runner-9'
    ).build(this)
}

def demoJob(projectName, portOffset, jobPrefix = "ansible-") {
  new ansible.MoleculeBuilder(
      projectName: projectName,
      moleculeBuildId: portOffset,
      jobPrefix: jobPrefix,
      pathToScript: 'ansible/molecule/molecule.sh',
      gitUrl: buildGitUrl(projectName),
      podmanImage: 'localhost/molecule-runner-9'
  ).build(this)
}

def downstreamRunnerJob(projectName, playbook, collections, productPaths) {
  new ansible.RunnerBuilder(
        projectName: projectName,
        playbook: playbook,
        collections: collections,
        products_paths: productPaths,
        podmanImage: 'localhost/molecule-runner-9',
        pathToScript: 'ansible/validate-downstream-collection.sh',
        jobPrefix: 'ansible-downstream-runner-',
        checkoutProject: "False"
    ).build(this)
}

def releaseCollection(projectName, projectUpstreamName = projectName) {
  new ansible.AnsibleReleaseBuilder(
        projectName: projectName,
        projectUpstreamName: projectUpstreamName
  ).build(this)
}
releaseCollection("eap","wildfly")
releaseCollection("jws","jws")
releaseCollection("data_grid", "infinispan")
releaseCollection("sso","keycloak")
releaseCollection("rhbk","keycloak")
releaseCollection("amq_broker","amq")
releaseCollection("amq_streams","amq_streams")

//releaseCollection("runtimes_common","common")
new ansible.AnsibleReleaseBuilder(
    projectName: "runtimes_common",
    projectUpstreamName: "common",
    pipelineFile: 'pipelines/ansible-release-common-pipeline',
    fullRelease: 'False'
    ).build(this)

EapView.jobList(this, 'Ansible Release', 'ansible-release.*')
//
// CI Jobs for Ansible Middleware
//
int upstreamProjectsPortOffsetstart = 22000

[ 'jws': 'jws', 'wildfly': 'eap', 'infinispan': 'data_grid', 'amq_streams': 'amq_streams'].each { project, upstreamProject -> upstreamCIJob(project, upstreamProject, upstreamProjectsPortOffsetstart++) }
upstreamCIJob('keycloak', 'sso', upstreamProjectsPortOffsetstart++, "default,overridexml")
upstreamCIJob('amq', 'amq_broker', upstreamProjectsPortOffsetstart++ , "default,amq_upgrade")
moleculeJobWithGitUrl('zeus', upstreamProjectsPortOffsetstart++, 'https://github.com/jboss-set/zeus.git', 'main')
moleculeJobWithGitUrl('common-criteria', upstreamProjectsPortOffsetstart++, 'https://github.com/ansible-middleware/common_criteria.git')
EapView.jobList(this, 'Ansible CI', 'ansible-ci.*')
//
// CI jobs for downstream (Janus generated) collections
//
int downstreamProjectsPortOffsetstart = 23000
['jws', 'eap', 'data_grid','sso', 'amq_streams'].each { project -> downstreamCIJob(project, downstreamProjectsPortOffsetstart++) }
downstreamCIJob('sso', downstreamProjectsPortOffsetstart++, "default,overridexml")
downstreamCIJob('rhbk', downstreamProjectsPortOffsetstart++, "quarkus")
downstreamCIJob('amq_broker', downstreamProjectsPortOffsetstart++, "default,amq_upgrade")
EapView.jobList(this, 'Ansible Downstream CI', 'ansible-downstream-ci.*$')

//
// DOT jobs
//
String dotJobsPrefix = "ansible-downstream-tests-"
int dotPortOffsetstart = 24000
['jws', 'eap', 'sso', 'amq_broker', 'rhbk', 'amq_streams'].each {  projectName -> dotJob(projectName, dotJobsPrefix, dotPortOffsetstart++) }
EapView.jobList(this, 'Ansible DOT', dotJobsPrefix + '.*$')

//
// CI Jobs for demos
//
int demoPortOffsetstart = 25000
[ 'wildfly-cluster-demo', 'flange-demo', 'eap-migration-demo', 'jws-app-update-demo'].each {  projectName -> demoJob(projectName, demoPortOffsetstart++) }
EapView.jobList(this, 'Ansible Demos', '^.*-demo')

//
// Janus jobs - generating downstream collections
//
janusJob('jws', projectUpstreamName: 'jws')
janusJob('eap', projectUpstreamName: 'wildfly')
janusJob('data_grid', projectUpstreamName: 'infinispan')
janusJob('sso', projectUpstreamName: 'keycloak')
janusJob('rhbk', projectUpstreamName: 'keycloak', upstreamCollectionName: 'keycloak')
janusJob('runtimes_common', projectUpstreamName: 'common')
janusJob('amq_broker', projectUpstreamName: 'activemq', upstreamCollectionName: 'amq')
janusJob('amq_streams', projectUpstreamName: 'amq_streams')
janusJob('openshift', projectUpstreamName: 'okd', gitUrl: 'https://github.com/openshift/community.okd.git', setupTrigger: false)
janusJob('ocpv', projectUpstreamName: 'kubevirt', upstreamCollectionName: 'kubevirt.core', gitUrl: 'https://github.com/kubevirt/kubevirt.core.git', setupTrigger: false)
EapView.jobList(this, 'Ansible Janus', '^ansible-janus.*$')
//
// Job testing the default playbook of the downstream (Janus generated) collection
//
downstreamRunnerJob('jws','playbook', 'redhat_csp_download', '/webserver/5.6.0/jws-5.6.0-application-server.zip,/webserver/5.6.0/jws-5.6.0-application-server-RHEL8-x86_64.zip')
downstreamRunnerJob('eap', 'playbook', 'redhat_csp_download', '/eap7/7.4.0/jboss-eap-7.4.0.zip')
downstreamRunnerJob('sso', 'keycloak', 'runtimes_common', '/sso/7.6.0/rh-sso-7.6.0-server-dist.zip')
downstreamRunnerJob('amq_broker', 'activemq', 'runtimes_common', '/amq/broker/7.9.4/amq-broker-7.9.4-bin.zip')
downstreamRunnerJob('amq_streams', '', 'runtimes_common', '/amq/streams/2.5.1/')
downstreamRunnerJob('rhbk', 'keycloak_quarkus', 'runtimes_common', '/keycloak/22.0.6/rhbk-22.0.6.zip')
downstreamRunnerJob('data_grid', 'infinispan', 'runtimes_common', '/datagrid/7.3.11/jboss-datagrid-7.3.11-server.zip')
EapView.jobList(this, 'Ansible Downstream Runner', '^ansible-downstream-runner-.*$')
