import io.quarkiverse.githubapp.event.PullRequest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestFileDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ConventionalCommitsValidator {

    private static final Pattern CONVENTIONAL_COMMIT_PATTERN = Pattern.compile("^(feat|fix|docs|style|refactor|test|chore)(\\(.*\\))?: .*");
    private static final Logger LOGGER = LoggerFactory.getLogger(ConventionalCommitsValidator.class);


    @Inject
    @RestClient
    OpenAIService openAIService;

    void onPullRequest(@PullRequest.Opened GHEventPayload.PullRequest payload) throws IOException {
        validateAndCommentOnPR(payload);
    }

    void onPullRequestUpdate(@PullRequest.Synchronize GHEventPayload.PullRequest payload) throws IOException {
        validateAndCommentOnPR(payload);
    }

    void onPullRequestEdit(@PullRequest.Edited GHEventPayload.PullRequest payload) throws IOException {
        validateAndCommentOnPR(payload);
    }

    private void validateAndCommentOnPR(GHEventPayload.PullRequest payload) throws IOException {
        GHPullRequest pullRequest = payload.getPullRequest();
        LOGGER.info("Processing pull request #{} - {}", pullRequest.getNumber(), pullRequest.getTitle());

        String title = payload.getPullRequest().getTitle();

        if (!isConventionalCommit(title)) {
            commentOnPullRequest(pullRequest, "The current pull request title does not follow the conventional commit format.");

            // Get the list of changed files with details
            List<GHPullRequestFileDetail> changedFiles = pullRequest.listFiles().toList();
            LOGGER.debug("Found {} changed files in pull request #{}", changedFiles.size(), pullRequest.getNumber());

            // Build a detailed context for OpenAI with content changes
            String changesContext = generateChangesContext(changedFiles);

            // Generate a suggested title using OpenAI
            String suggestedTitle = suggestTitleWithAI(changesContext);

            // Comment the suggested title on the pull request
            commentOnPullRequest(pullRequest, "Suggested title based on the changes:\n" + suggestedTitle);

            LOGGER.info("Suggested title: {}", suggestedTitle);
            updatePullRequestTitle(pullRequest, suggestedTitle);

        } else {
            commentOnPullRequest(pullRequest, "The pull request title follows the conventional commit format.");
        }
    }

    private boolean isConventionalCommit(String title) {
        return CONVENTIONAL_COMMIT_PATTERN.matcher(title).matches();
    }

    /**
     * Generates a detailed context of file changes for OpenAI.
     */
    private String generateChangesContext(List<GHPullRequestFileDetail> changedFiles) throws IOException {
        return changedFiles.stream()
                .map(file -> {
                    // Retrieve file content changes (diff)
                    String patch = file.getPatch();
                    return String.format("File: %s\nChanges:\n%s", file.getFilename(), patch);
                })
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * Suggests a pull request title using OpenAI by providing detailed file changes.
     */
    private String suggestTitleWithAI(String changesContext) {
        // Construct the prompt for OpenAI
        String prompt = "Based on the following file changes, suggest a title for this pull request that adheres to the Conventional Commits format:\n\n"
                + changesContext;

        LOGGER.debug("Sending prompt to OpenAI: {}", prompt);

        // Build the AIRequest
        AIRequest request = new AIRequest.Builder()
                .model("gpt-4o")
                .messages(List.of(
                        new AIRequest.Message("system", "You are an assistant specializing in pull request titles."),
                        new AIRequest.Message("user", prompt)
                ))
                .build();

        // Call the OpenAI API
        AIResponse response = openAIService.generateTitle(request);

        // Validate and return the response content
        if (response.getChoices() != null && !response.getChoices().isEmpty()) {
            var suggestedTitle = response.getChoices().getFirst().getMessage().getContent();
            LOGGER.debug("Received suggested title from OpenAI: {}", suggestedTitle);

            return suggestedTitle;
        } else {
            LOGGER.error("No response received from OpenAI.");
            throw new RuntimeException("No response received from OpenAI.");
        }
    }

    /**
     * Updates the pull request title with the suggested title.
     */
    private void updatePullRequestTitle(GHPullRequest pullRequest, String suggestedTitle) throws IOException {
        // Set the new title for the pull request
        pullRequest.setTitle(suggestedTitle);
        LOGGER.info("Pull request #{} title updated to: {}", pullRequest.getNumber(), suggestedTitle);
    }

    /**
     * Posts a comment on the pull request.
     */
    private void commentOnPullRequest(GHPullRequest pullRequest, String comment) throws IOException {
        pullRequest.comment(comment);
        LOGGER.debug("Comment posted on pull request #{}: {}", pullRequest.getNumber(), comment);
    }

}