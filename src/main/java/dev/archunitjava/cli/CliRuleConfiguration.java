package dev.archunitjava.cli;

import dev.archunitjava.result.Severity;
import dev.archunitjava.rules.DependencyRuleMode;
import dev.archunitjava.rules.ExternalDependencyPolicy;
import dev.archunitjava.rules.SelfDependencyPolicy;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;

/** Whitelisted declarative dependency or cycle rule configuration. */
public record CliRuleConfiguration(
        String id,
        CliGraphDomain domain,
        DependencyRuleMode mode,
        CliPattern origins,
        CliPattern targets,
        SelfDependencyPolicy selfDependencies,
        ExternalDependencyPolicy externalDependencies,
        Optional<String> displayName,
        Optional<String> rationale,
        List<String> tags,
        Severity severity,
        CliRuleKind kind) implements Comparable<CliRuleConfiguration> {
    /** Retains the dependency-rule constructor used before cycle configurations were introduced. */
    public CliRuleConfiguration(
            String id, CliGraphDomain domain, DependencyRuleMode mode, CliPattern origins,
            CliPattern targets, SelfDependencyPolicy selfDependencies,
            ExternalDependencyPolicy externalDependencies, Optional<String> displayName,
            Optional<String> rationale, List<String> tags, Severity severity) {
        this(id, domain, mode, origins, targets, selfDependencies, externalDependencies,
                displayName, rationale, tags, severity, CliRuleKind.DEPENDENCY);
    }

    public CliRuleConfiguration {
        if (id == null || !id.matches("[A-Za-z0-9._-]{1,64}")) {
            throw new CliConfigurationException("Invalid rule id: " + id);
        }
        Objects.requireNonNull(domain, "domain");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(origins, "origins");
        Objects.requireNonNull(targets, "targets");
        Objects.requireNonNull(selfDependencies, "selfDependencies");
        Objects.requireNonNull(externalDependencies, "externalDependencies");
        displayName = text(displayName, "displayName");
        rationale = text(rationale, "rationale");
        Objects.requireNonNull(tags, "tags");
        TreeSet<String> normalizedTags = new TreeSet<>();
        for (String tag : tags) {
            if (tag == null || !tag.matches("[A-Za-z0-9._-]{1,64}")) {
                throw new CliConfigurationException("Invalid tag for rule " + id + ": " + tag);
            }
            normalizedTags.add(tag);
        }
        tags = List.copyOf(normalizedTags);
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(kind, "kind");
        if (kind == CliRuleKind.CYCLES && (mode != DependencyRuleMode.NO
                || !origins.equals(targets) || selfDependencies != SelfDependencyPolicy.IGNORE
                || externalDependencies != ExternalDependencyPolicy.IGNORE)) {
            throw new CliConfigurationException("Cycle rules use one induced selection and default cycle policies");
        }
    }

    @Override
    public int compareTo(CliRuleConfiguration other) {
        return id.compareTo(other.id);
    }

    private static Optional<String> text(Optional<String> value, String role) {
        Objects.requireNonNull(value, role);
        return value.map(item -> {
            if (item.isBlank() || item.length() > 4096) {
                throw new CliConfigurationException(role + " must contain 1 to 4096 characters");
            }
            return item;
        });
    }
}
