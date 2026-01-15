package icyllis.modernui.mc.mixin;

import icyllis.modernui.mc.MuiModApi;
import icyllis.modernui.mc.ScrollController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Mixin(AbstractSelectionList.class)
public abstract class MixinSelectionList extends AbstractContainerWidget implements ScrollController.IListener {

    public MixinSelectionList(int x, int y, int width, int height, Component msg) {
        super(x, y, width, height, msg, AbstractScrollArea.defaultSettings(40));
    }

    @Shadow
    @Final
    protected Minecraft minecraft;

    @Unique
    @Nullable
    private ScrollController modernUI_MC$scrollController;

    @Unique
    private boolean modernUI_MC$callSuperSetScrollAmount;

    @Invoker("repositionEntries")
    protected abstract void modernUI_MC$invokeRepositionEntries();

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.visible) {
            return false;
        }
        if (scrollY != 0) {
            if (modernUI_MC$scrollController == null) {
                return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
            }
            modernUI_MC$scrollController.setMaxScroll(maxScrollAmount());
            if (!modernUI_MC$scrollController.scrollBy((float) (-scrollY * 40.0))) {
                return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
            }
            return true;
        }
        return false;
    }

    @Inject(method = "renderWidget", at = @At("HEAD"))
    private void preRender(GuiGraphics gr, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (modernUI_MC$scrollController == null) {
            modernUI_MC$scrollController = new ScrollController(this);
            modernUI_MC$skipAnimationTo(scrollAmount());
        }
        modernUI_MC$scrollController.update(MuiModApi.getElapsedTime());
    }

    @Inject(method = "renderWidget", at = @At(value = "INVOKE", shift = At.Shift.BEFORE, target = "Lnet/minecraft/client" +
            "/gui/components/AbstractSelectionList;renderListItems(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
    private void preRenderList(@Nonnull GuiGraphics gr, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        gr.pose().pushMatrix();
        gr.pose().translate(0,
                ((int) (((int) scrollAmount() - scrollAmount()) * (float) minecraft.getWindow().getGuiScale())) / (float) minecraft.getWindow().getGuiScale());
    }

    @Inject(method = "renderWidget", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/client" +
            "/gui/components/AbstractSelectionList;renderListItems(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
    private void postRenderList(@Nonnull GuiGraphics gr, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        gr.pose().popMatrix();
    }

    @Override
    public void setScrollAmount(double target) {
        if (modernUI_MC$scrollController != null && !modernUI_MC$callSuperSetScrollAmount) {
            modernUI_MC$skipAnimationTo(target);
        } else {
            super.setScrollAmount(target);
            modernUI_MC$invokeRepositionEntries();
        }
    }

    @Override
    public void onScrollAmountUpdated(ScrollController controller, float amount) {
        modernUI_MC$callSuperSetScrollAmount = true;
        setScrollAmount(amount);
        modernUI_MC$callSuperSetScrollAmount = false;
    }

    @Unique
    public void modernUI_MC$skipAnimationTo(double target) {
        assert modernUI_MC$scrollController != null;
        modernUI_MC$scrollController.setMaxScroll(maxScrollAmount());
        modernUI_MC$scrollController.scrollTo((float) target);
        modernUI_MC$scrollController.abortAnimation();
    }
}
