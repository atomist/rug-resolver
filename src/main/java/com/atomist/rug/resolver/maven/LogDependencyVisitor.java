package com.atomist.rug.resolver.maven;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;

public class LogDependencyVisitor implements DependencyVisitor {

    private Log out;
    private DependencyVisitor visitor;

    private List<ChildInfo> childInfos = new ArrayList<ChildInfo>();

    public LogDependencyVisitor(Log out) {
        this.out = out;
    }

    public LogDependencyVisitor(Log out, DependencyVisitor visitor) {
        this.out = out;
        this.visitor = visitor;
    }

    public boolean visitEnter(DependencyNode node) {
        out.info(formatIndentation() + formatNode(node));
        childInfos.add(new ChildInfo(node.getChildren().size()));
        return (visitor != null ? visitor.visitEnter(node) : true);
    }

    private String formatIndentation() {
        StringBuilder buffer = new StringBuilder(128);
        for (Iterator<ChildInfo> it = childInfos.iterator(); it.hasNext();) {
            buffer.append(it.next().formatIndentation(!it.hasNext()));
        }
        return buffer.toString();
    }

    private String formatNode(DependencyNode node) {
        StringBuilder buffer = new StringBuilder(128);
        Artifact a = node.getArtifact();
        Dependency d = node.getDependency();
        buffer.append(a);
        if (d != null && d.getScope().length() > 0) {
            buffer.append(" [").append(d.getScope());
            if (d.isOptional()) {
                buffer.append(", optional");
            }
            buffer.append("]");
        }
        {
            String premanaged = DependencyManagerUtils.getPremanagedVersion(node);
            if (premanaged != null && !premanaged.equals(a.getBaseVersion())) {
                buffer.append(" (version managed from ").append(premanaged).append(")");
            }
        }
        {
            String premanaged = DependencyManagerUtils.getPremanagedScope(node);
            if (premanaged != null && !premanaged.equals(d.getScope())) {
                buffer.append(" (scope managed from ").append(premanaged).append(")");
            }
        }
        DependencyNode winner = (DependencyNode) node.getData()
                .get(ConflictResolver.NODE_DATA_WINNER);
        if (winner != null && !ArtifactIdUtils.equalsId(a, winner.getArtifact())) {
            Artifact w = winner.getArtifact();
            buffer.append(" (conflicts with ");
            if (ArtifactIdUtils.toVersionlessId(a).equals(ArtifactIdUtils.toVersionlessId(w))) {
                buffer.append(w.getVersion());
            }
            else {
                buffer.append(w);
            }
            buffer.append(")");
        }
        return buffer.toString();
    }

    public boolean visitLeave(DependencyNode node) {
        if (!childInfos.isEmpty()) {
            childInfos.remove(childInfos.size() - 1);
        }
        if (!childInfos.isEmpty()) {
            childInfos.get(childInfos.size() - 1).index++;
        }
        return (visitor != null ? visitor.visitLeave(node) : true);
    }

    private static class ChildInfo {

        final int count;

        int index;

        public ChildInfo(int count) {
            this.count = count;
        }

        public String formatIndentation(boolean end) {
            boolean last = index + 1 >= count;
            if (end) {
                return last ? "└─ " : "├─ ";
            }
            return last ? "   " : "|  ";
        }
    }
    
    public interface Log {
        
        void info(String message);
        
    }
}
