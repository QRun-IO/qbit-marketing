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

package com.kingsrook.qbits.marketing.security;


import java.util.List;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.DenyBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;


/*******************************************************************************
 ** The Marketing permissions. Every name starts with the full qbit name, and
 ** the suffixes are QQQ's standard ones (read / write for tables, hasAccess for
 ** apps, processes and widgets), so a host grants them like any other QQQ
 ** permission.
 **
 ** Preparation, approval and execution are separate permissions: a user who
 ** may draft content (and run the agent) cannot approve it without
 ** marketing.approve.hasAccess, and cannot publish it without
 ** marketing.execute.hasAccess.
 *******************************************************************************/
public final class MarketingPermissions
{
   public static final String BASE         = "marketing";
   public static final String ADMIN_BASE   = "marketing.admin";
   public static final String PREPARE_BASE = "marketing.prepare";
   public static final String APPROVE_BASE = "marketing.approve";
   public static final String EXECUTE_BASE = "marketing.execute";
   public static final String AGENT_BASE   = "marketing.agent";

   public static final String APP_ACCESS = BASE + ".hasAccess";
   public static final String READ       = BASE + ".read";
   public static final String WRITE      = BASE + ".write";
   public static final String ADMIN_READ = ADMIN_BASE + ".read";
   public static final String ADMIN_WRITE = ADMIN_BASE + ".write";
   public static final String PREPARE    = PREPARE_BASE + ".hasAccess";
   public static final String APPROVE    = APPROVE_BASE + ".hasAccess";
   public static final String EXECUTE    = EXECUTE_BASE + ".hasAccess";
   public static final String AGENT      = AGENT_BASE + ".hasAccess";

   public static final List<String> ALL = List.of(APP_ACCESS, READ, WRITE, ADMIN_READ, ADMIN_WRITE, PREPARE, APPROVE, EXECUTE, AGENT);



   /*******************************************************************************
    ** Utility class - no instances.
    *******************************************************************************/
   private MarketingPermissions()
   {
   }



   /*******************************************************************************
    ** Does the current session hold one of the permissions? The system user
    ** (scheduler) does.
    *******************************************************************************/
   public static boolean has(String... permissions)
   {
      QSession session = QContext.getQSession();
      if(session instanceof QSystemUserSession)
      {
         return (true);
      }
      if(session == null)
      {
         return (false);
      }
      for(String permission : permissions)
      {
         if(session.hasPermission(permission))
         {
            return (true);
         }
      }
      return (false);
   }



   /*******************************************************************************
    ** Require one of the permissions (defense in depth behind process rules).
    *******************************************************************************/
   public static void require(String what, String... permissions) throws QUserFacingException
   {
      if(!has(permissions))
      {
         throw (new QUserFacingException("You do not have permission to " + what + " (" + String.join(" or ", permissions) + ")."));
      }
   }



   /*******************************************************************************
    ** Tables people edit: marketing.read / marketing.write.
    *******************************************************************************/
   public static QPermissionRules tableRules()
   {
      return (rules(PermissionLevel.READ_WRITE_PERMISSIONS, BASE));
   }



   /*******************************************************************************
    ** Settings tables (channels, policy): marketing.admin.read / .write.
    *******************************************************************************/
   public static QPermissionRules adminTableRules()
   {
      return (rules(PermissionLevel.READ_WRITE_PERMISSIONS, ADMIN_BASE));
   }



   /*******************************************************************************
    ** Apps, processes and widgets: <base>.hasAccess.
    *******************************************************************************/
   public static QPermissionRules hasAccessRules(String base)
   {
      return (rules(PermissionLevel.HAS_ACCESS_PERMISSION, base));
   }



   /*******************************************************************************
    ** Explicit level and deny behavior: left null, the enricher would copy the
    ** host's default (often NOT_PROTECTED).
    *******************************************************************************/
   private static QPermissionRules rules(PermissionLevel level, String base)
   {
      return (new QPermissionRules()
         .withLevel(level)
         .withPermissionBaseName(base)
         .withDenyBehavior(DenyBehavior.HIDDEN));
   }

}
