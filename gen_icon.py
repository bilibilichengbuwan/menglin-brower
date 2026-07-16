#!/usr/bin/env python3
import math, struct, zlib, os

def make_png(filename, size, draw):
    raw = b''
    for y in range(size):
        raw += b'\x00'
        for x in range(size):
            r, g, b, a = draw(x, y, size)
            raw += bytes([r, g, b, a])
    def chunk(tag, data):
        return struct.pack('>I', len(data)) + tag + data + struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff)
    sig = b'\x89PNG\r\n\x1a\n'
    ihdr = struct.pack('>IIBBBBB', size, size, 8, 6, 0, 0, 0)
    idat = zlib.compress(raw)
    png = sig + chunk(b'IHDR', ihdr) + chunk(b'IDAT', idat) + chunk(b'IEND', b'')
    with open(filename, 'wb') as f:
        f.write(png)

# Edge-style palette: deep blue -> medium blue -> cyan -> light cyan
COLOR_DEEP_BLUE = (5, 50, 155)
COLOR_DARK_BLUE = (25, 90, 200)
COLOR_MID_BLUE = (55, 145, 235)
COLOR_LIGHT_BLUE = (110, 200, 250)
COLOR_CYAN = (160, 230, 255)
COLOR_WHITE = (255, 255, 255)

def lerp(c1, c2, t):
    t = max(0, min(1, t))
    return tuple(int(c1[i] + (c2[i] - c1[i]) * t) for i in range(3))

def draw_icon(x, y, size):
    cx, cy = size / 2, size / 2

    # Rounded square mask (Edge-style rounded corners)
    half = size * 0.49
    corner_radius = size * 0.18

    # Compute distance from rounded square boundary
    abs_dx = abs(x - cx)
    abs_dy = abs(y - cy)

    # Check if inside rounded square
    if abs_dx > half or abs_dy > half:
        # Check corner regions
        corner_x = abs_dx - (half - corner_radius)
        corner_y = abs_dy - (half - corner_radius)
        if corner_x > 0 and corner_y > 0:
            corner_dist = math.sqrt(corner_x * corner_x + corner_y * corner_y)
            if corner_dist > corner_radius:
                return (0, 0, 0, 0)
            # Anti-aliasing near corner edge
            if corner_dist > corner_radius - 1.5:
                alpha = int(255 * (corner_radius - corner_dist + 1.5) / 1.5)
                alpha = max(0, min(255, alpha))
            else:
                alpha = 255
        elif abs_dx > half or abs_dy > half:
            return (0, 0, 0, 0)
        else:
            alpha = 255
    else:
        alpha = 255

    # Edge anti-aliasing on straight edges
    edge_distance = min(half - abs_dx, half - abs_dy)
    if edge_distance < 1.5:
        alpha = int(alpha * min(1.0, edge_distance / 1.5 + 0.3))

    # Background gradient - from bottom-left (deep blue) to top-right (cyan/light blue)
    # Normalize position
    nx = (x - cx) / half  # -1 to 1
    ny = (y - cy) / half  # -1 to 1

    # Diagonal gradient like Edge logo
    # Bottom-left (-1, 1) -> dark blue; Top-right (1, -1) -> cyan
    grad = (nx - ny) / 2.0  # -1 to 1 range
    grad_norm = (grad + 1) / 2.0  # 0 to 1

    if grad_norm < 0.25:
        bg = lerp(COLOR_DEEP_BLUE, COLOR_DARK_BLUE, grad_norm / 0.25)
    elif grad_norm < 0.5:
        bg = lerp(COLOR_DARK_BLUE, COLOR_MID_BLUE, (grad_norm - 0.25) / 0.25)
    elif grad_norm < 0.75:
        bg = lerp(COLOR_MID_BLUE, COLOR_LIGHT_BLUE, (grad_norm - 0.5) / 0.25)
    else:
        bg = lerp(COLOR_LIGHT_BLUE, COLOR_CYAN, (grad_norm - 0.75) / 0.25)

    # Add subtle central brightness boost
    center_dist = math.sqrt(nx * nx + ny * ny)
    center_boost = max(0, 1 - center_dist * 0.9) * 0.1
    bg = (
        min(255, int(bg[0] + center_boost * 255)),
        min(255, int(bg[1] + center_boost * 255)),
        min(255, int(bg[2] + center_boost * 255))
    )

    # Edge-style wave pattern (the stylized "e")
    # Create a curved wave using parametric approach
    # The wave flows from top-left area, curves to the right and down

    # Parametric wave - create region boundaries using wave functions
    wave_y = ny  # vertical position normalized

    # Create the main wave curve - a flowing curve
    # The Edge logo has a thick wave crossing diagonally
    curve_amplitude = 0.35
    curve_frequency = 2.2
    curve_phase = 0.5

    # Main wave centerline (the center of the white wave)
    wave_center = curve_amplitude * math.sin(nx * curve_frequency + curve_phase) - 0.15

    # Wave thickness
    wave_thickness = 0.25

    # Distance from current point to wave centerline (perpendicular-ish)
    dist_to_wave = abs(ny - wave_center)

    # Create tapered wave (thicker on the left, thinner on the right)
    taper = 0.8 - nx * 0.4  # thick on left, tapering right
    effective_thickness = wave_thickness * max(0.3, taper)

    # Determine if inside white wave
    if dist_to_wave < effective_thickness and nx < 0.7 and nx > -0.95:
        # Check distance from edges for anti-aliasing
        edge_dist = effective_thickness - dist_to_wave
        # Clamp to edges
        if nx > 0.65:
            edge_dist = min(edge_dist, (0.75 - nx) * 1.5)
        if nx < -0.9:
            edge_dist = min(edge_dist, (nx + 0.85) * 1.5)

        if edge_dist < 0.05:
            # Anti-aliased edge
            blend = edge_dist / 0.05
            blend = max(0, min(1, blend))
            # Light cyan glow at edges
            edge_color = lerp(bg, COLOR_CYAN, 0.3 + blend * 0.3)
            r = int(edge_color[0] * (1 - blend) + COLOR_WHITE[0] * blend)
            g = int(edge_color[1] * (1 - blend) + COLOR_WHITE[1] * blend)
            b = int(edge_color[2] * (1 - blend) + COLOR_WHITE[2] * blend)
            return (r, g, b, alpha)
        else:
            return (COLOR_WHITE[0], COLOR_WHITE[1], COLOR_WHITE[2], alpha)

    # Small dark blue segment at bottom (like the Edge logo tail)
    tail_curve = curve_amplitude * 0.7 * math.sin(nx * 2.5 + 1.8) + 0.55
    dist_to_tail = abs(ny - tail_curve)
    tail_thickness = 0.12 * max(0.5, 0.8 - nx * 0.3)

    if dist_to_tail < tail_thickness and nx < 0.4 and nx > -0.7:
        edge_dist_t = tail_thickness - dist_to_tail
        if edge_dist_t < 0.06:
            blend = edge_dist_t / 0.06
            blend = max(0, min(1, blend))
            dark = (10, 40, 130)
            r = int(bg[0] * (1 - blend) + dark[0] * blend)
            g = int(bg[1] * (1 - blend) + dark[1] * blend)
            b = int(bg[2] * (1 - blend) + dark[2] * blend)
            return (r, g, b, alpha)
        else:
            dark = (10, 40, 130)
            return (dark[0], dark[1], dark[2], alpha)

    return (bg[0], bg[1], bg[2], alpha)


base = '/workspace/yulan/res'
sizes = {
    'mipmap-mdpi': 48,
    'mipmap-hdpi': 72,
    'mipmap-xhdpi': 96,
    'mipmap-xxhdpi': 144,
    'mipmap-xxxhdpi': 192,
}
for folder, sz in sizes.items():
    d = os.path.join(base, folder)
    os.makedirs(d, exist_ok=True)
    make_png(os.path.join(d, 'ic_launcher.png'), sz, draw_icon)
    make_png(os.path.join(d, 'ic_launcher_round.png'), sz, draw_icon)
print("Icons generated")
