package net.minecraftforge.gradle.common.runtime.spec;

import net.minecraftforge.gradle.dsl.common.runtime.tasks.Runtime;
import net.minecraftforge.gradle.dsl.common.tasks.WithOutput;
import net.minecraftforge.gradle.dsl.common.util.GameArtifact;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Defines a callback which is invoked to handle modifications to an MCP task tree.
 */
@FunctionalInterface
public interface TaskTreeAdapter {

    /**
     * Invoked to get a task which is run after the taskOutputs of which the output is given.
     * The invoker is responsible for registering the task to the project, which is retrievable via {@link  CommonRuntimeSpec#project()}.
     *
     * @param spec The runtime spec to build a task for.
     * @param previousTasksOutput The previous task build output.
     * @return The task to run.
     */
    @Nullable
    TaskProvider<? extends Runtime> adapt(final CommonRuntimeSpec spec, final Provider<? extends WithOutput> previousTasksOutput, final File runtimeWorkspace, final Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifacts, final Map<String, String> mappingVersionData, final Consumer<TaskProvider<? extends Runtime>> dependentTaskConfigurationHandler);

    /**
     * Runs the given task adapter after the current one.
     * Implicitly chaining the build output of this adapters task as the input for the given adapters task.
     * Automatically configures the task tree dependencies.
     *
     * @param after The task tree adapter to run afterwards.
     * @return The combined task tree adapter.
     */
    @NotNull
    default TaskTreeAdapter andThen(final TaskTreeAdapter after) {
        Objects.requireNonNull(after);
        return (spec, previousTaskOutput, runtimeWorkspace, gameArtifactTaskProviderMap, mappingVersionData, dependentTaskConfigurationHandler) -> {
            final TaskProvider<? extends Runtime> currentAdapted = TaskTreeAdapter.this.adapt(spec, previousTaskOutput, runtimeWorkspace, gameArtifactTaskProviderMap, mappingVersionData, dependentTaskConfigurationHandler);

            if (currentAdapted != null)
                dependentTaskConfigurationHandler.accept(currentAdapted);

            final TaskProvider<? extends Runtime> afterAdapted = after.adapt(spec, currentAdapted, runtimeWorkspace, gameArtifactTaskProviderMap, mappingVersionData, dependentTaskConfigurationHandler);

            if (currentAdapted != null && afterAdapted != null)
                afterAdapted.configure(task -> task.dependsOn(currentAdapted));

            return afterAdapted;
        };
    }

}
