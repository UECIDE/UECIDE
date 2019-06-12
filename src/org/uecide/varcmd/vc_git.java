/*
 * Copyright (c) 2019, Majenko Technologies
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * 
 * * Redistributions in binary form must reproduce the above copyright notice, this
 *   list of conditions and the following disclaimer in the documentation and/or
 *   other materials provided with the distribution.
 * 
 * * Neither the name of Majenko Technologies nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.uecide.varcmd;

import org.uecide.*;
import java.io.File;

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.*;
import org.eclipse.jgit.internal.storage.file.*;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.transport.OpenSshConfig.*;
import org.eclipse.jgit.util.FS;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.KeyPair;


public class vc_git implements VariableCommand {

    File dotGit;
    FileRepository localRepo;
    File repoRoot;
    Git git;

    public String main(Context sketch, String args) {

        if (!openRepo(sketch.getSketch().getFolder())) {
            return "ERR:NOTGIT";
        }
        if (args.equals("hash")) {
            return getLatestCommit();
        }
        if (args.equals("describe")) {
            return getDescription();
        }
        return "ERR:NOKEY";
    }

    public String getLatestCommit() {
        try {
            LogCommand cmd = git.log();

            cmd.setMaxCount(1);

            Iterable<RevCommit> it = cmd.call();

            for (RevCommit commit : it) {
                return commit.getId().toString();
            }
            return "ERR:NOCOMMIT";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "ERR:EXCEPT";
    }

    public String getDescription() {
        try {
            DescribeCommand cmd = git.describe();
            String desc = cmd.call();
            return desc;
        } catch (Exception e) {
            e.printStackTrace();
            return "ERR:NOTAG";
        }
    }

    public boolean openRepo(File where) {
        try {
            File here = where;
            dotGit = new File(here, ".git");

            JSch.setConfig("StrictHostKeyChecking", "no");
            while (!dotGit.exists()) {
                here = here.getParentFile();
                if (here == null) {
                    localRepo = null;
                    return false;
                }
                dotGit = new File(here, ".git");
            }

            repoRoot = here;

            localRepo = new FileRepository(dotGit);
            git = new Git(localRepo);
        } catch (Exception e) {
            Base.error(e);
            localRepo = null;
            repoRoot = null;
            return false;
        }
        return true;
    }

}
