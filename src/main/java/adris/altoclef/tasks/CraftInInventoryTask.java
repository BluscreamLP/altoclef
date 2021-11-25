package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.resources.CollectRecipeCataloguedResourcesTask;
import adris.altoclef.tasks.slot.EnsureFreeInventorySlotTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.*;

/**
 * Crafts an item within the 2x2 inventory crafting grid.
 */
public class CraftInInventoryTask extends ResourceTask {

    private final CraftingRecipe _recipe;
    private final boolean _collect;
    private final boolean _ignoreUncataloguedSlots;
    private boolean _fullCheckFailed = false;
    private long missingTicks = 0;

    public CraftInInventoryTask(ItemTarget target, CraftingRecipe recipe, boolean collect, boolean ignoreUncataloguedSlots) {
        super(target);
        _recipe = recipe;
        _collect = collect;
        _ignoreUncataloguedSlots = ignoreUncataloguedSlots;
    }

    public CraftInInventoryTask(ItemTarget target, CraftingRecipe recipe) {
        this(target, recipe, true, false);
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        _fullCheckFailed = false;
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        ItemTarget toGet = _itemTargets[0];

        if (mod.getInventoryTracker().hasRecipeMaterialsOrTarget(new RecipeTarget(toGet, _recipe)) && !mod.getInventoryTracker().isFullyCapableToCraft(mod, new RecipeTarget(toGet, _recipe))) {
            this.missingTicks++;
        } else {
            this.missingTicks = 0;
        }

        if (this.missingTicks > 150) {
            if (Utils.isNull(mod.getInventoryTracker().getMissingItemTarget(mod, _recipe))) {
                this.missingTicks = 0;
                return null;
            }

            if (Utils.isNull(mod.getInventoryTracker().getMissingItemTarget(mod, _recipe).getMatches())) throw new IllegalStateException("why are missing matches null?");

            return TaskCatalogue.getItemTask(mod.getInventoryTracker().getMissingItemTarget(mod, _recipe));
        }

        if (_collect && !mod.getInventoryTracker().hasRecipeMaterialsOrTarget(new RecipeTarget(toGet, _recipe)) /*|| this.missingTicks > 250*//*!isFullyCapableToCraft(mod, _recipe)*/) {
            setDebugState("Collecting materials");
            return collectRecipeSubTask(mod);
        }

        // Free up inventory
        if (mod.getInventoryTracker().isInventoryFull()) {
            return new EnsureFreeInventorySlotTask();
        }

        setDebugState("Crafting in inventory... for " + toGet);

        return new CraftGenericTask(_recipe);
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        if (other instanceof CraftInInventoryTask task) {
            if (!task._recipe.equals(_recipe)) return false;
            return isCraftingEqual(task);
        }
        return false;
    }

    @Override
    protected String toDebugStringName() {
        return toCraftingDebugStringName() + " " + _recipe;
    }

    // virtual. By default assumes subtasks are CATALOGUED (in TaskCatalogue.java)
    protected Task collectRecipeSubTask(AltoClef mod) {
        return new CollectRecipeCataloguedResourcesTask(_ignoreUncataloguedSlots, new RecipeTarget(_itemTargets[0], _recipe));
    }

    protected String toCraftingDebugStringName() {
        return "Craft 2x2 Task";
    }

    protected boolean isCraftingEqual(CraftInInventoryTask other) {
        return true;
    }
}
