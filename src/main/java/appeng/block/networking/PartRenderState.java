package appeng.block.networking;

import appeng.api.parts.IPartItem;
import appeng.util.render.AERenderData;

public record PartRenderState(IPartItem<?> partItem, AERenderData renderData) {
}
