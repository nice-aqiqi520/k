// Copyright (c) 2014-2015 K Team. All Rights Reserved.
package org.kframework.parser.concrete2kore.kernel;

import dk.brics.automaton.Automaton;
import org.kframework.attributes.Location;
import org.kframework.attributes.Source;
import org.kframework.definition.Production;
import org.kframework.parser.Constant;
import org.kframework.parser.KList;
import org.kframework.parser.Term;
import org.kframework.parser.TermCons;
import org.pcollections.ConsPStack;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * An action that transforms an AST into another AST
 */
public abstract class Rule implements Serializable {
    /**
     * Metadata used to inform a rule about the current parse.
     */
    static class MetaData {
        public static class Location {
            public final int position;
            public final int line;
            public final int column;

            public Location(int position, int line, int column) {
                this.position = position;
                this.line = line;
                this.column = column;
            }
        }
        public final Source source;
        public final Location start;
        public final Location end;
        public final CharSequence input;
        public MetaData(Source source, Location start, Location end, CharSequence input) {
            assert start != null && end != null && source != null;
            this.source = source;
            this.start = start;
            this.end = end;
            this.input = input;
        }
    }

    /**
     * A rule who's action does not depend on the context in which the parse occurs.
     */
    public static abstract class ContextFreeRule extends Rule {
        public abstract Set<Term> apply(Set<Term> set, MetaData metaData);
    }

    /**
     * Helper class for rules that treat each KList passed to apply() independently from each other
     */
    public static abstract class KListRule extends ContextFreeRule {
        public Set<Term> apply(Set<Term> set, MetaData metaData) {
            Set<Term> result = new HashSet<>();
            for (Term klist : set) {
                Term newKList = this.apply((KList)klist, metaData);
                if (newKList != null) {
                    result.add(newKList);
                }
            }
            return result;
        }
        protected abstract KList apply(KList set, MetaData metaData);
    }

    /**
     * Wraps the current KList with the given KLabel
     */
    public static class WrapLabelRule extends KListRule {
        private final Production label;
        public final Automaton rejectPattern;
        private final Set<String> rejects;
        public WrapLabelRule(Production label, Automaton rejectPattern, Set<String> rejects) {
            assert label != null;
            this.label = label;
            this.rejectPattern = rejectPattern;
            this.rejects = rejects;
        }
        public WrapLabelRule(Production label) {
            this(label, null, new HashSet<>());
        }
        protected KList apply(KList klist, MetaData metaData) {
            Term term;
            Location loc = new Location(metaData.start.line, metaData.start.column, metaData.end.line, metaData.end.column);
            Source source = metaData.source;
            if (label.att().contains("token")) {
                String value = metaData.input.subSequence(metaData.start.position, metaData.end.position).toString();
                if (rejectPattern != null && rejectPattern.run(value)) {
                    return null;
                }
                if (rejects.contains(value)) {
                    return null;
                }
                term = Constant.apply(value, label, Optional.of(loc), Optional.of(source));
            } else {
                term = TermCons.apply(klist.items(), label, loc, source);
            }
            return new KList(ConsPStack.singleton(term));
        }
    }

    /**
     * Delete the last few elements added to the KList.
     * Usually used to remove whitespace and tokens
     */
    public static class DeleteRule extends KListRule {
        private final int length;
        public DeleteRule(int length) {
            this.length = length;
        }


        @Override
        protected KList apply(KList set, MetaData metaData) {
            return set.remove(length);
        }
    }
}
