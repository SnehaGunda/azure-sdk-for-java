// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.ai.textanalytics.lro;

import com.azure.ai.textanalytics.TextAnalyticsClient;
import com.azure.ai.textanalytics.TextAnalyticsClientBuilder;
import com.azure.ai.textanalytics.models.AnalyzeBatchActionsOperationDetail;
import com.azure.ai.textanalytics.models.AnalyzeBatchActionsOptions;
import com.azure.ai.textanalytics.models.AnalyzeBatchActionsResult;
import com.azure.ai.textanalytics.models.CategorizedEntity;
import com.azure.ai.textanalytics.models.ExtractKeyPhraseResult;
import com.azure.ai.textanalytics.models.ExtractKeyPhrasesActionResult;
import com.azure.ai.textanalytics.models.ExtractKeyPhrasesOptions;
import com.azure.ai.textanalytics.models.RecognizeEntitiesActionResult;
import com.azure.ai.textanalytics.models.RecognizeEntitiesOptions;
import com.azure.ai.textanalytics.models.RecognizeEntitiesResult;
import com.azure.ai.textanalytics.models.TextAnalyticsActions;
import com.azure.ai.textanalytics.models.TextDocumentInput;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.http.rest.PagedIterable;
import com.azure.core.http.rest.PagedResponse;
import com.azure.core.util.Context;
import com.azure.core.util.polling.LongRunningOperationStatus;
import com.azure.core.util.polling.SyncPoller;

import java.util.ArrayList;
import java.util.List;

/**
 * Sample demonstrates how to synchronously execute actions in a batch of documents, such as key phrases extraction,
 * PII entities recognition actions.
 */
public class AnalyzeBatchActions {

    /**
     * Main method to invoke this demo about how to analyze a batch of tasks.
     *
     * @param args Unused arguments to the program.
     */
    public static void main(String[] args) {
        TextAnalyticsClient client = new TextAnalyticsClientBuilder()
                                         .credential(new AzureKeyCredential("{key}"))
                                         .endpoint("{endpoint}")
                                         .buildClient();

        List<TextDocumentInput> documents = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            documents.add(new TextDocumentInput(Integer.toString(i),
                "We went to Contoso Steakhouse located at midtown NYC last week for a dinner party, and we adore"
                    + " the spot! They provide marvelous food and they have a great menu. The chief cook happens to be"
                    + " the owner (I think his name is John Doe) and he is super nice, coming out of the kitchen and "
                    + "greeted us all. We enjoyed very much dining in the place! The Sirloin steak I ordered was tender"
                    + " and juicy, and the place was impeccably clean. You can even pre-order from their online menu at"
                    + " www.contososteakhouse.com, call 312-555-0176 or send email to order@contososteakhouse.com! The"
                    + " only complaint I have is the food didn't come fast enough. Overall I highly recommend it!"
            ));
        }

        SyncPoller<AnalyzeBatchActionsOperationDetail, PagedIterable<AnalyzeBatchActionsResult>> syncPoller =
            client.beginAnalyzeBatchActions(documents,
                new TextAnalyticsActions().setDisplayName("{tasks_display_name}")
                    .setRecognizeEntitiesOptions(new RecognizeEntitiesOptions())
                    .setExtractKeyPhrasesOptions(
                        new ExtractKeyPhrasesOptions().setModelVersion("invalidVersion"),
                        new ExtractKeyPhrasesOptions().setModelVersion("latest")),
                new AnalyzeBatchActionsOptions().setIncludeStatistics(false),
                Context.NONE);

        // Task operation statistics details
        while (syncPoller.poll().getStatus() == LongRunningOperationStatus.IN_PROGRESS) {
            final AnalyzeBatchActionsOperationDetail operationDetail = syncPoller.poll().getValue();
            System.out.printf("Action display name: %s, Successfully completed actions: %d, in-process actions: %d,"
                                  + " failed actions: %d, total actions: %d%n",
                operationDetail.getDisplayName(), operationDetail.getActionsSucceeded(),
                operationDetail.getActionsInProgress(), operationDetail.getActionsFailed(),
                operationDetail.getActionsInTotal());
        }

        syncPoller.waitForCompletion();

        Iterable<PagedResponse<AnalyzeBatchActionsResult>> pagedResults = syncPoller.getFinalResult().iterableByPage();
        for (PagedResponse<AnalyzeBatchActionsResult> perPage : pagedResults) {
            System.out.printf("Response code: %d, Continuation Token: %s.%n", perPage.getStatusCode(),
                perPage.getContinuationToken());
            for (AnalyzeBatchActionsResult actionsResult : perPage.getElements()) {
                System.out.println("Entities recognition action results:");
                for (RecognizeEntitiesActionResult actionResult : actionsResult.getRecognizeEntitiesActionResults()) {
                    if (!actionResult.isError()) {
                        for (RecognizeEntitiesResult documentResult : actionResult.getResult()) {
                            if (!documentResult.isError()) {
                                for (CategorizedEntity entity : documentResult.getEntities()) {
                                    System.out.printf(
                                        "\tText: %s, category: %s, confidence score: %f.%n",
                                        entity.getText(), entity.getCategory(), entity.getConfidenceScore());
                                }
                            } else {
                                System.out.printf("\tCannot recognize entities. Error: %s%n",
                                    documentResult.getError().getMessage());
                            }
                        }
                    } else {
                        System.out.printf("\tCannot execute Entities Recognition action. Error: %s%n",
                            actionResult.getError().getMessage());
                    }
                }

                System.out.println("Key phrases extraction action results:");
                for (ExtractKeyPhrasesActionResult actionResult : actionsResult.getExtractKeyPhrasesActionResults()) {
                    if (!actionResult.isError()) {
                        for (ExtractKeyPhraseResult documentResult : actionResult.getResult()) {
                            if (!documentResult.isError()) {
                                System.out.println("\tExtracted phrases:");
                                for (String keyPhrases : documentResult.getKeyPhrases()) {
                                    System.out.printf("\t\t%s.%n", keyPhrases);
                                }
                            } else {
                                System.out.printf("\tCannot extract key phrases. Error: %s%n",
                                    documentResult.getError().getMessage());
                            }
                        }
                    } else {
                        System.out.printf("\tCannot execute Key Phrases Extraction action. Error: %s%n",
                            actionResult.getError().getMessage());
                    }
                }
            }
        }
    }
}
