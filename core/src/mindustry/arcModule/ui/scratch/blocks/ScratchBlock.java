package mindustry.arcModule.ui.scratch.blocks;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Cell;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Nullable;
import mindustry.arcModule.ui.scratch.*;
import mindustry.arcModule.ui.scratch.elements.CondElement;
import mindustry.arcModule.ui.scratch.elements.InputElement;
import mindustry.arcModule.ui.scratch.elements.LabelElement;

public class ScratchBlock extends ScratchTable {
    public ScratchType type;
    private final BlockInfo info;
    public ScratchBlock linkTo, linkFrom;
    public byte dir = 0;
    public Seq<Element> elements = new Seq<>();

    public ScratchBlock(ScratchType type, Color color, BlockInfo info) {
        this(type, color, info, true);
    }

    public ScratchBlock(ScratchType type, Color color, BlockInfo info, boolean dragEnabled) {
        this.type = type;
        elemColor = color;
        this.info = info;
        info.build(this);
        if (children.size != 0) {
            if (children.get(0) instanceof InputElement e) {
                getCell(e).padLeft(addPadding + 10);
            }
            if (children.get(children.size - 1) instanceof InputElement e) {
                getCell(e).padRight(addPadding + 10);
            }
        }
        if (dragEnabled) ScratchInput.addDraggingInput(this);
    }

    public void label(String str) {
        LabelElement l = new LabelElement(str);
        l.cell(add(l));
    }

    public void cond() {
        CondElement e = new CondElement();
        e.cell(add(e));
    }

    public void input() {
        input(false);
    }

    public void input(boolean num) {
        input(num, "");
    }

    public void input(boolean num, String def) {
        InputElement e = new InputElement(num, def);
        Cell<ScratchTable> c;
        e.cell(c = add(e));
        if (children.size == 1) {
            c.padLeft(addPadding + 10);
        }
    }

    public ScratchBlock copy() {
        ScratchBlock sb = new ScratchBlock(type, elemColor, info);
        for (int i = 0; i < elements.size; i++) {
            Element child = elements.get(i);
            Element target = sb.elements.get(i);
            if (child instanceof ScratchTable st1 && target instanceof ScratchTable st2) {
                st2.setElementValue(st1.getElementValue());
                if (st1.child instanceof ScratchBlock sb2) st2.setChild(sb2.copy());
            }
        }
        return sb;
    }

    public void drawSuper() {
        super.draw();
    }

    public void linkFrom(@Nullable ScratchBlock source) {
        linkFrom = source;
    }

    public void linkTo(ScratchBlock target) {
        if (linkTo != null) unlink();
        target.linkFrom(this);
        linkTo = target;
    }

    public void insertLinkTop(ScratchBlock before) {
        if (before.linkTo != null) {
            linkTo(before.linkTo);
        } else {
            x = before.x;
            y = before.y;
        }
        ScratchBlock l = this;
        while (l.linkFrom != null) l = l.linkFrom;
        before.linkTo(l);
    }

    public void insertLinkBottom(ScratchBlock after) {
        ScratchBlock target = after.linkFrom;
        linkTo(after);
        ScratchBlock l = this;
        while (l.linkFrom != null) l = l.linkFrom;
        if (target != null) target.linkTo(l);
    }

    public void removeAndKeepLink() {
        boolean linked = linkFrom != null && linkTo != null;
        if (linked) {
            linkFrom.linkTo(linkTo);
        }
        if (linkFrom != null && linkTo == null) {
            linkFrom.x = x;
            linkFrom.y = y;
        }
        unlinkAll();
        remove();
    }

    public void unlink() {
        if (linkTo == null) return;
        if (linkTo.linkFrom == this) linkTo.linkFrom(null);
        linkTo = null;
    }

    public void unlinkFrom() {
        if (linkFrom == null) return;
        if (linkFrom.linkTo == this) linkFrom.unlink();
    }

    public void unlinkAll() {
        unlink();
        unlinkFrom();
    }

    @Override
    public boolean acceptLink(ScratchBlock block) {
        return type == ScratchType.block;
    }

    @Override
    public Object getValue() {
        if (type == ScratchType.block) return null;
        return info.getValue(elements);
    }

    @Override
    public ScratchType getType() {
        return type;
    }

    @Override
    public Element hit(float x, float y, boolean touchable) {
        if (!(ScratchController.dragging instanceof ScratchBlock b && b.type == ScratchType.block) || ScratchController.dragging == linkTo || ScratchController.dragging == linkFrom) return super.hit(x, y, touchable);
        if(touchable && this.touchable != Touchable.enabled) return null;
        if (x >= 0 && x < width) {
            if (y >= -padValue * 2 && y < 0) {
                dir = Align.bottom;
                return this;
            }
            if ((linkTo == null || linkTo instanceof FakeBlock) && y >= height + padValue && y < height + padValue * 2 + (linkTo != null ? linkTo.getHeight() : 0)) {
                dir = Align.top;
                return this;
            }
        }
        return super.hit(x, y, touchable);
    }

    @Override
    public void draw() {
        Draw.reset();
        switch (type) {
            case input -> ScratchStyles.drawInput(x, y, width, height, elemColor);
            case condition, conditionInner -> ScratchStyles.drawCond(x, y, width, height, elemColor);
            case block -> ScratchStyles.drawBlock(x, y, width, height, elemColor, false);
        }
        super.draw();
        if (ScratchController.dragging instanceof ScratchBlock b && b.type == ScratchType.block && type == ScratchType.block) {
            Lines.stroke(1f);
            Draw.color(Color.gold.cpy().mulA(0.5f));
            Lines.rect(x, y - padValue * 2, width, padValue);
            if (linkTo == null || linkTo instanceof FakeBlock) {
                Draw.color(Color.blue.cpy().mulA(0.5f));
                Lines.rect(x, y + padValue, width, height + padValue * 2);
            }
        }
    }

    @Override
    public void act(float delta) {
        if (linkTo == null) {
            ScratchBlock b = this;
            do {
                b.toFront();
                b = b.linkFrom;
            } while (b != null);
        }
        if (linkTo != null) {
            x = linkTo.x;
            y = linkTo.y - getHeight() + 7f;
        }
        super.act(delta);
    }

    @Override
    public boolean remove() {
        unlinkAll();
        if (parent != null) return parent.removeChild(this);
        return false;
    }

    @Override
    public void addChild(Element actor) {
        super.addChild(actor);
        elements.add(actor);
    }
}
