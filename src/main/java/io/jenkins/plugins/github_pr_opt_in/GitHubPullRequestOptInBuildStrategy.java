package io.jenkins.plugins.github_pr_opt_in;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.TaskListener;
import jenkins.branch.BranchBuildStrategy;
import jenkins.branch.BranchBuildStrategyDescriptor;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceDescriptor;
import org.jenkinsci.plugins.github_branch_source.Connector;
import org.jenkinsci.plugins.github_branch_source.GitHubSCMSource;
import org.jenkinsci.plugins.github_branch_source.PullRequestSCMHead;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GitHub;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * {@link BranchBuildStrategy} that only builds pull requests with specific label set.
 */
public class GitHubPullRequestOptInBuildStrategy extends BranchBuildStrategy {

    private String optInLabel;

    @DataBoundConstructor
    public GitHubPullRequestOptInBuildStrategy(@Nonnull String optInLabel) {
        this.optInLabel = optInLabel;
    }

    public String getOptInLabel() {
        return optInLabel;
    }

    @Override
    public boolean isAutomaticBuild(@Nonnull SCMSource scmSource, @Nonnull SCMHead scmHead, @Nonnull SCMRevision scmRevision, SCMRevision scmRevision1, SCMRevision scmRevision2, @Nonnull TaskListener taskListener) {
        try {
            if (!(scmHead instanceof PullRequestSCMHead) || !(scmSource instanceof GitHubSCMSource)) {
                return false; // don't trigger non-PR
            }
            PullRequestSCMHead head = (PullRequestSCMHead) scmHead;
            GitHubSCMSource gitHubSCMSource = (GitHubSCMSource) scmSource;

            final String credentialsId = gitHubSCMSource.getCredentialsId();
            final GitHub github = Connector.connect(gitHubSCMSource.getApiUri(), Connector.lookupScanCredentials((Item) scmSource.getOwner(), gitHubSCMSource.getApiUri(), credentialsId));

            final String fullRepoName = gitHubSCMSource.getRepoOwner() + "/" + gitHubSCMSource.getRepository();
            GHPullRequest pr = github.getRepository(fullRepoName).getPullRequest(head.getNumber());
            if (pr.getLabels().stream().anyMatch(l -> l.getName().equals(optInLabel))) {
                return true;
            }
        } catch (IOException e) {
            taskListener.error("Failed to determine labels for pull request: " + e.getMessage());
        }
        return false;
    }

    @Extension
    public static class DescriptorImpl extends BranchBuildStrategyDescriptor {
        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.GitHubPullRequestOptInBuildStrategy_DisplayName();
        }

        @Override
        public boolean isApplicable(@Nonnull SCMSourceDescriptor sourceDescriptor) {
            return sourceDescriptor instanceof GitHubSCMSource.DescriptorImpl;
        }
    }
}
