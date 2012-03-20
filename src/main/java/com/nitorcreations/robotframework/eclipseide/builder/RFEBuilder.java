/**
 * Copyright 2012 Nitor Creations Oy
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
package com.nitorcreations.robotframework.eclipseide.builder;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import com.nitorcreations.robotframework.eclipseide.Activator;
import com.nitorcreations.robotframework.eclipseide.builder.parser.RFELine;
import com.nitorcreations.robotframework.eclipseide.builder.parser.RFEParser;
import com.nitorcreations.robotframework.eclipseide.builder.parser.RobotFile;

public class RFEBuilder extends IncrementalProjectBuilder {

    abstract class BaseResourceVisitor {
        final Set<IFile> visitedFiles = new LinkedHashSet<IFile>();
    }

    class ResourceVisitor extends BaseResourceVisitor implements IResourceVisitor {

        private final IProgressMonitor monitor;

        public ResourceVisitor(IProgressMonitor monitor) {
            this.monitor = monitor;
        }

        @Override
        public boolean visit(IResource resource) {
            parse(visitedFiles, resource, monitor);
            // return true to continue visiting children.
            return true;
        }

    }

    class ResourceDeltaVisitor extends BaseResourceVisitor implements IResourceDeltaVisitor {

        private final IProgressMonitor monitor;

        public ResourceDeltaVisitor(IProgressMonitor monitor) {
            this.monitor = monitor;
        }

        @Override
        public boolean visit(IResourceDelta delta) throws CoreException {
            IResource resource = delta.getResource();
            switch (delta.getKind()) {
            case IResourceDelta.ADDED:
                // handle added resource
                parse(visitedFiles, resource, monitor);
                break;
            case IResourceDelta.REMOVED:
                // handle removed resource
                if (resource instanceof IFile) {
                    RobotFile.erase((IFile) resource);
                }
                break;
            case IResourceDelta.CHANGED:
                // handle changed resource
                parse(visitedFiles, resource, monitor);
                break;
            }
            // return true to continue visiting children.
            return true;
        }

    }

    public static final String BUILDER_ID = Activator.PLUGIN_ID + ".rfeBuilder";

    public static final String MARKER_TYPE = Activator.PLUGIN_ID + ".testProblem";

    @Override
    protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
        if (kind == FULL_BUILD) {
            fullBuild(monitor);
        } else {
            IResourceDelta delta = getDelta(getProject());
            if (delta == null) {
                fullBuild(monitor);
            } else {
                incrementalBuild(delta, monitor);
            }
        }
        return null;
    }

    protected void fullBuild(final IProgressMonitor monitor) throws CoreException {
        System.out.println("<full-build>");
        ResourceVisitor visitor = new ResourceVisitor(monitor);
        getProject().accept(visitor);
        updateProblems(visitor.visitedFiles, monitor);
        System.out.println("</full-build>");
    }

    protected void incrementalBuild(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
        System.out.println("<incremental-build>");
        ResourceDeltaVisitor visitor = new ResourceDeltaVisitor(monitor);
        delta.accept(visitor);
        updateProblems(visitor.visitedFiles, monitor);
        System.out.println("</incremental-build>");
    }

    /**
     * This is called for every resource that has changed (for example when
     * saving a file or refreshing resources).
     * 
     * @param resource
     *            the resource that changed
     * @param monitor
     *            progress monitor
     */
    void parse(Set<IFile> visitedFiles, IResource resource, IProgressMonitor monitor) {
        if (!(resource instanceof IFile))
            return;
        IFile file = (IFile) resource;
        if (!file.getName().endsWith(".txt") || file.getProjectRelativePath().toPortableString().startsWith("target/")) {
            try {
                file.deleteMarkers(RFEBuilder.MARKER_TYPE, false, IResource.DEPTH_ZERO);
            } catch (CoreException e) {
                // ignore
            }
            return;
        }
        System.out.println("Build resource " + resource);
        try {
            RobotFile.parse(file, monitor);
        } catch (Exception e1) {
            // new Status(IStatus.WARNING, Activator.PLUGIN_ID,
            // "Internal error", e1)
            throw new RuntimeException("Parsing problem", e1);
        }
        visitedFiles.add(file);
    }

    private void updateProblems(Set<IFile> visitedFiles, IProgressMonitor monitor) {
        System.out.println(" -- updating problems");
        // TODO recursively add files that include the visited files to the
        // visited files list
        for (IFile file : visitedFiles) {
            try {
                List<RFELine> lines = RobotFile.get(file, false).getLines();
                new RFEParser(file, lines, monitor).parse();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
