package com.example.blockshooter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

/**
 * Transparent overlay. While playing: floating joystick (left), look-drag (right),
 * fire / jump / reload / pause buttons, crosshair, health, ammo, objective.
 * In every other state it draws the menu / pause / result screens with buttons.
 */
public class HudView extends View {
    private static class Btn {
        float l, t, r, b;
        String label;
        int cmd;
        int color;
    }

    private final GameInput input;
    private final float d;
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ammoText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cross = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint crossShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint infoText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bigText = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int movePointer = -1, lookPointer = -1;
    private int jumpPointer = -1, firePointer = -1, reloadPointer = -1;
    private float baseX, baseY, curX, curY;
    private float lastLookX, lastLookY;
    private float lastFireX, lastFireY;

    public HudView(Context context, GameInput input) {
        super(context);
        this.input = input;
        d = context.getResources().getDisplayMetrics().density;

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(3f * d);
        stroke.setColor(0x88FFFFFF);

        fill.setStyle(Paint.Style.FILL);

        text.setColor(0xFFFFFFFF);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(15f * d);

        ammoText.setColor(0xFFFFFFFF);
        ammoText.setTextAlign(Paint.Align.CENTER);
        ammoText.setTextSize(26f * d);
        ammoText.setShadowLayer(4f, 1f, 1f, 0xFF000000);

        cross.setColor(0xFFFFFFFF);
        cross.setStrokeWidth(2f * d);
        crossShadow.setColor(0xAA000000);
        crossShadow.setStrokeWidth(4f * d);

        bar.setStyle(Paint.Style.FILL);

        infoText.setColor(0xFFFFFFFF);
        infoText.setTextAlign(Paint.Align.CENTER);
        infoText.setTextSize(16f * d);
        infoText.setShadowLayer(4f, 1f, 1f, 0xFF000000);

        hitPaint.setStrokeWidth(3f * d);

        bigText.setColor(0xFFFFFFFF);
        bigText.setTextAlign(Paint.Align.CENTER);
        bigText.setTextSize(34f * d);
        bigText.setShadowLayer(6f, 2f, 2f, 0xFF000000);
    }

    // ---------- geometry ----------
    private float joyRadius() { return 60f * d; }
    private float fireX() { return getWidth() - 90f * d; }
    private float fireY() { return getHeight() - 100f * d; }
    private float fireR() { return 50f * d; }
    private float jumpX() { return getWidth() - 90f * d; }
    private float jumpY() { return getHeight() - 215f * d; }
    private float jumpR() { return 36f * d; }
    private float reloadX() { return getWidth() - 195f * d; }
    private float reloadY() { return getHeight() - 70f * d; }
    private float reloadR() { return 30f * d; }
    private float pauseX() { return getWidth() - 40f * d; }
    private float pauseY() { return 40f * d; }
    private float pauseR() { return 22f * d; }

    private static boolean inCircle(float x, float y, float cx, float cy, float r) {
        float dx = x - cx, dy = y - cy;
        return dx * dx + dy * dy <= r * r * 1.5f;
    }

    // ---------- menu buttons ----------
    private Btn btn(float cx, float top, float w, float h, String label, int cmd, int color) {
        Btn b = new Btn();
        b.l = cx - w / 2f;
        b.r = cx + w / 2f;
        b.t = top;
        b.b = top + h;
        b.label = label;
        b.cmd = cmd;
        b.color = color;
        return b;
    }

    private ArrayList<Btn> buildButtons() {
        ArrayList<Btn> list = new ArrayList<>();
        float cx = getWidth() / 2f;
        float W = 240f * d, H = 38f * d, gap = 8f * d;
        float y = getHeight() * 0.38f;
        int st = input.state;
        int GREEN = 0xFF2E9E4F, BLUE = 0xFF2F6FD0, RED = 0xFFC0392B, GRAY = 0xFF555555;

        if (st == GameInput.STATE_MENU) {
            y = getHeight() * 0.40f;
            if (input.savedLevel > 1) {
                list.add(btn(cx, y, W, H, "متابعة (المرحلة " + input.savedLevel + ")", GameInput.CMD_CONTINUE, GREEN));
                y += H + gap;
                list.add(btn(cx, y, W, H, "لعبة جديدة", GameInput.CMD_NEW_GAME, BLUE));
            } else {
                list.add(btn(cx, y, W, H, "ابدأ اللعب", GameInput.CMD_NEW_GAME, GREEN));
            }
            y += H + gap;
        } else if (st == GameInput.STATE_PAUSED) {
            list.add(btn(cx, y, W, H, "متابعة", GameInput.CMD_RESUME, GREEN));
            y += H + gap;
            list.add(btn(cx, y, W, H, "إعادة المرحلة", GameInput.CMD_RETRY, BLUE));
            y += H + gap;
            list.add(btn(cx, y, W, H, "القائمة الرئيسية", GameInput.CMD_MENU, RED));
            y += H + gap;
        } else if (st == GameInput.STATE_LEVEL_CLEAR) {
            y = getHeight() * 0.50f;
            list.add(btn(cx, y, W, H, "المرحلة التالية", GameInput.CMD_NEXT_LEVEL, GREEN));
            y += H + gap;
            list.add(btn(cx, y, W, H, "القائمة الرئيسية", GameInput.CMD_MENU, GRAY));
            y += H + gap;
            return list;
        } else if (st == GameInput.STATE_DEAD) {
            y = getHeight() * 0.50f;
            list.add(btn(cx, y, W, H, "إعادة المرحلة", GameInput.CMD_RETRY, GREEN));
            y += H + gap;
            list.add(btn(cx, y, W, H, "القائمة الرئيسية", GameInput.CMD_MENU, GRAY));
            y += H + gap;
            return list;
        } else if (st == GameInput.STATE_VICTORY) {
            y = getHeight() * 0.55f;
            list.add(btn(cx, y, W, H, "القائمة الرئيسية", GameInput.CMD_MENU, GREEN));
            return list;
        } else {
            return list;
        }

        // sound / vibration toggles (menu + pause)
        float half = (W - 8f * d) / 2f;
        Btn s = btn(cx - half / 2f - 4f * d, y, half, H,
                "الصوت: " + (input.soundOn ? "تشغيل" : "إيقاف"), GameInput.CMD_TOGGLE_SOUND,
                input.soundOn ? GRAY : 0xFF333333);
        Btn v = btn(cx + half / 2f + 4f * d, y, half, H,
                "الاهتزاز: " + (input.vibOn ? "تشغيل" : "إيقاف"), GameInput.CMD_TOGGLE_VIB,
                input.vibOn ? GRAY : 0xFF333333);
        list.add(s);
        list.add(v);
        return list;
    }

    private void drawButtons(Canvas c) {
        for (Btn b : buildButtons()) {
            fill.setColor(b.color);
            c.drawRoundRect(b.l, b.t, b.r, b.b, 10f * d, 10f * d, fill);
            text.setTextSize(14f * d);
            c.drawText(b.label, (b.l + b.r) / 2f, (b.t + b.b) / 2f + 5f * d, text);
        }
        text.setTextSize(15f * d);
    }

    private String formatTime(float t) {
        int s = (int) t;
        return (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60);
    }

    // ---------- drawing ----------
    private void button(Canvas c, float x, float y, float r, boolean pressed, int color, String label) {
        fill.setColor(pressed ? (color | 0xAA000000) : ((color & 0x00FFFFFF) | 0x44000000));
        c.drawCircle(x, y, r, fill);
        c.drawCircle(x, y, r, stroke);
        c.drawText(label, x, y + 5f * d, text);
    }

    @Override
    protected void onDraw(Canvas c) {
        int w = getWidth(), h = getHeight();
        int st = input.state;
        long now = System.nanoTime();

        if (st == GameInput.STATE_PLAYING || st == GameInput.STATE_PAUSED) {
            drawGameplay(c, w, h, now);
        }

        // overlays
        if (st != GameInput.STATE_PLAYING) {
            c.drawColor(st == GameInput.STATE_MENU ? 0x66000000 : 0xAA000000);
            bigText.setTextSize(34f * d);
            infoText.setTextSize(16f * d);

            if (st == GameInput.STATE_MENU) {
                bigText.setColor(0xFFFFFFFF);
                bigText.setTextSize(44f * d);
                c.drawText("BLOCK SHOOTER", w / 2f, h * 0.22f, bigText);
                infoText.setTextSize(16f * d);
                c.drawText("لعبة القتال بالمكعبات", w / 2f, h * 0.22f + 30f * d, infoText);
                if (input.savedLevel > 1) {
                    infoText.setTextSize(13f * d);
                    c.drawText("وصلت إلى المرحلة " + input.savedLevel + " من " + input.totalLevels,
                            w / 2f, h * 0.22f + 52f * d, infoText);
                }
            } else if (st == GameInput.STATE_PAUSED) {
                bigText.setColor(0xFFFFFFFF);
                c.drawText("متوقف مؤقتاً", w / 2f, h * 0.22f, bigText);
            } else if (st == GameInput.STATE_LEVEL_CLEAR) {
                bigText.setColor(0xFF66FF66);
                c.drawText("اكتملت المرحلة " + input.level + "!", w / 2f, h * 0.24f, bigText);
                infoText.setTextSize(18f * d);
                c.drawText("القتلى: " + input.levelKills + "   |   الوقت: " + formatTime(input.levelTime),
                        w / 2f, h * 0.36f, infoText);
            } else if (st == GameInput.STATE_DEAD) {
                bigText.setColor(0xFFFF5555);
                c.drawText("انتهت اللعبة", w / 2f, h * 0.26f, bigText);
                infoText.setTextSize(16f * d);
                c.drawText("المرحلة " + input.level + "  |  القتلى: " + input.levelKills + " / " + input.levelEnemies,
                        w / 2f, h * 0.38f, infoText);
            } else if (st == GameInput.STATE_VICTORY) {
                bigText.setColor(0xFFFFDD55);
                c.drawText("مبروك! أنهيت اللعبة", w / 2f, h * 0.26f, bigText);
                infoText.setTextSize(17f * d);
                c.drawText("أكملت جميع المراحل الـ " + input.totalLevels, w / 2f, h * 0.37f, infoText);
                c.drawText("آخر مرحلة: القتلى " + input.levelKills + "  |  الوقت " + formatTime(input.levelTime),
                        w / 2f, h * 0.37f + 28f * d, infoText);
            }
            drawButtons(c);
        }

        postInvalidateDelayed(50);
    }

    private void drawGameplay(Canvas c, int w, int h, long now) {
        // crosshair
        float cx = w / 2f, cy = h / 2f, s = 10f * d;
        c.drawLine(cx - s, cy, cx + s, cy, crossShadow);
        c.drawLine(cx, cy - s, cx, cy + s, crossShadow);
        c.drawLine(cx - s, cy, cx + s, cy, cross);
        c.drawLine(cx, cy - s, cx, cy + s, cross);

        // joystick
        if (movePointer != -1) {
            fill.setColor(0x22FFFFFF);
            c.drawCircle(baseX, baseY, joyRadius(), fill);
            c.drawCircle(baseX, baseY, joyRadius(), stroke);
            fill.setColor(0x88FFFFFF);
            c.drawCircle(curX, curY, 26f * d, fill);
        } else {
            c.drawCircle(110f * d, h - 100f * d, joyRadius(), stroke);
        }

        // buttons
        button(c, fireX(), fireY(), fireR(), firePointer != -1, 0xFFFF4444, "نار");
        button(c, jumpX(), jumpY(), jumpR(), jumpPointer != -1, 0xFFFFFFFF, "قفز");
        button(c, reloadX(), reloadY(), reloadR(), reloadPointer != -1, 0xFF44AAFF, "تعبئة");
        button(c, pauseX(), pauseY(), pauseR(), false, 0xFFFFFFFF, "II");

        // ammo
        String label;
        if (input.reloading) {
            ammoText.setColor(0xFFFFDD55);
            label = "جاري التعبئة...";
        } else {
            ammoText.setColor(input.ammo == 0 ? 0xFFFF5555 : 0xFFFFFFFF);
            label = input.ammo + " / " + input.reserve;
        }
        c.drawText(label, w / 2f, h - 24f * d, ammoText);

        // health bar
        float bw = 190f * d, bh = 14f * d;
        float bx0 = w / 2f - bw / 2f, by0 = h - 78f * d;
        bar.setColor(0x88000000);
        c.drawRect(bx0 - 2f * d, by0 - 2f * d, bx0 + bw + 2f * d, by0 + bh + 2f * d, bar);
        float frac = Math.max(0f, Math.min(1f, input.health / 100f));
        bar.setColor(frac > 0.5f ? 0xFF44CC44 : (frac > 0.25f ? 0xFFFFAA22 : 0xFFEE3333));
        c.drawRect(bx0, by0, bx0 + bw * frac, by0 + bh, bar);
        infoText.setTextSize(13f * d);
        c.drawText("الصحة " + input.health, w / 2f, by0 - 6f * d, infoText);

        // objective (top centre)
        infoText.setTextSize(16f * d);
        c.drawText("المرحلة " + input.level + "   |   الأعداء: " + input.enemies, w / 2f, 30f * d, infoText);

        // level banner for the first seconds
        float bannerAge = (now - input.bannerTime) / 1e9f;
        if (input.bannerTime != 0L && bannerAge < 4f) {
            int alpha = (int) (255f * Math.min(1f, (4f - bannerAge)));
            bigText.setTextSize(30f * d);
            bigText.setColor((alpha << 24) | 0x00FFFFFF);
            c.drawText("المرحلة " + input.level, w / 2f, h * 0.26f, bigText);
            infoText.setTextSize(17f * d);
            infoText.setAlpha(alpha);
            c.drawText("الهدف: اقضِ على جميع الجنود (" + input.levelEnemies + ")", w / 2f, h * 0.26f + 30f * d, infoText);
            infoText.setAlpha(255);
        }

        // hit marker
        float hitAge = (now - input.hitTime) / 1e9f;
        if (input.hitTime != 0L && hitAge < 0.18f) {
            hitPaint.setColor(input.hitKill ? 0xFFFF3333 : (input.hitHead ? 0xFFFFDD33 : 0xFFFFFFFF));
            float a = 8f * d, b2 = 18f * d;
            c.drawLine(cx - b2, cy - b2, cx - a, cy - a, hitPaint);
            c.drawLine(cx + b2, cy - b2, cx + a, cy - a, hitPaint);
            c.drawLine(cx - b2, cy + b2, cx - a, cy + a, hitPaint);
            c.drawLine(cx + b2, cy + b2, cx + a, cy + a, hitPaint);
        }

        // damage flash
        float dmgAge = (now - input.damageTime) / 1e9f;
        if (input.damageTime != 0L && dmgAge < 0.5f) {
            int alpha = (int) ((1f - dmgAge / 0.5f) * 110f);
            c.drawColor((alpha << 24) | 0x00FF0000);
        }
    }

    // ---------- touch ----------
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int action = e.getActionMasked();
        int idx = e.getActionIndex();
        int w = getWidth();

        // menus / overlays
        if (input.state != GameInput.STATE_PLAYING) {
            movePointer = lookPointer = jumpPointer = firePointer = reloadPointer = -1;
            input.reset();
            if (action == MotionEvent.ACTION_DOWN) {
                float x = e.getX(), y = e.getY();
                for (Btn b : buildButtons()) {
                    if (x >= b.l && x <= b.r && y >= b.t && y <= b.b) {
                        Sfx.play(Sfx.CLICK, 0.6f);
                        input.postCommand(b.cmd);
                        break;
                    }
                }
            }
            return true;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int id = e.getPointerId(idx);
                float x = e.getX(idx), y = e.getY(idx);
                if (inCircle(x, y, pauseX(), pauseY(), pauseR())) {
                    input.postCommand(GameInput.CMD_PAUSE);
                } else if (firePointer == -1 && inCircle(x, y, fireX(), fireY(), fireR())) {
                    firePointer = id;
                    lastFireX = x;
                    lastFireY = y;
                    input.fire = true;
                } else if (jumpPointer == -1 && inCircle(x, y, jumpX(), jumpY(), jumpR())) {
                    jumpPointer = id;
                    input.jump = true;
                } else if (reloadPointer == -1 && inCircle(x, y, reloadX(), reloadY(), reloadR())) {
                    reloadPointer = id;
                    input.requestReload();
                } else if (x < w * 0.45f && movePointer == -1) {
                    movePointer = id;
                    baseX = curX = x;
                    baseY = curY = y;
                    input.moveX = 0f;
                    input.moveY = 0f;
                } else if (x >= w * 0.45f && lookPointer == -1) {
                    lookPointer = id;
                    lastLookX = x;
                    lastLookY = y;
                }
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                for (int i = 0; i < e.getPointerCount(); i++) {
                    int id = e.getPointerId(i);
                    float x = e.getX(i), y = e.getY(i);
                    if (id == movePointer) {
                        float dx = x - baseX, dy = y - baseY;
                        float len = (float) Math.sqrt(dx * dx + dy * dy);
                        float r = joyRadius();
                        if (len > r) {
                            dx = dx / len * r;
                            dy = dy / len * r;
                        }
                        curX = baseX + dx;
                        curY = baseY + dy;
                        input.moveX = dx / r;
                        input.moveY = -dy / r;
                    } else if (id == lookPointer) {
                        input.addLook(x - lastLookX, y - lastLookY);
                        lastLookX = x;
                        lastLookY = y;
                    } else if (id == firePointer) {
                        input.addLook(x - lastFireX, y - lastFireY);
                        lastFireX = x;
                        lastFireY = y;
                    }
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP: {
                releasePointer(e.getPointerId(idx));
                break;
            }
            case MotionEvent.ACTION_CANCEL:
                movePointer = lookPointer = jumpPointer = firePointer = reloadPointer = -1;
                input.reset();
                break;
        }
        invalidate();
        return true;
    }

    private void releasePointer(int id) {
        if (id == movePointer) {
            movePointer = -1;
            input.moveX = 0f;
            input.moveY = 0f;
        } else if (id == lookPointer) {
            lookPointer = -1;
        } else if (id == jumpPointer) {
            jumpPointer = -1;
            input.jump = false;
        } else if (id == firePointer) {
            firePointer = -1;
            input.fire = false;
        } else if (id == reloadPointer) {
            reloadPointer = -1;
        }
    }
}
