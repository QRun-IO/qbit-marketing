/*
 * Copyright 2026 Kingsrook, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kingsrook.qbits.marketing.agent.tools;


import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.ai.AiToolKind;
import com.kingsrook.qqq.backend.core.exceptions.QException;


/*******************************************************************************
 ** A narrow, typed tool of the marketing agent.
 **
 ** - READ tools return marketing data the requesting user may read (record
 **   locks and core permission enforcement apply). They are also contributed
 **   to the QQQ AI tool kernel (KernelToolRegistration), so the assistant
 **   panel can use them.
 ** - WRITE_PROPOSAL tools only create drafts and findings: never approvals,
 **   schedules or executions. They run only inside the marketing agent.
 **
 ** Inputs are validated against the schema's property names by the runner
 ** and checked again by the tool; outputs are plain maps that the runner
 ** redacts, caps and wraps as untrusted data.
 *******************************************************************************/
public interface MarketingAgentTool
{

   /*******************************************************************************
    ** lower_snake_case name, prefixed marketing_.
    *******************************************************************************/
   String getName();

   /*******************************************************************************
    ** What the tool does, for the model.
    *******************************************************************************/
   String getDescription();

   /*******************************************************************************
    ** READ or WRITE_PROPOSAL.
    *******************************************************************************/
   AiToolKind getKind();

   /*******************************************************************************
    ** JSON schema of the input (an object).
    *******************************************************************************/
   Map<String, Object> getInputSchema();

   /*******************************************************************************
    ** Run it. Throw QUserFacingException (or AiToolException) for invalid
    ** input; the runner maps exceptions to outcomes.
    *******************************************************************************/
   Map<String, Object> run(MarketingToolContext context, Map<String, Object> input) throws QException;

}
