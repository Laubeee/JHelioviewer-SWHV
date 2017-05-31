void get_lati_texcoord(float ln, float lt, float cr, const in vec4 rect, out vec2 texcoord, out float radius) {
    vec2 normalizedScreenpos = (gl_FragCoord.xy - viewportOffset) / viewport - .5;
    normalizedScreenpos *= 2. * vec2(viewport.y / viewport.x, 1.);
    vec4 scrpos = cameraTransformationInverse * vec4(normalizedScreenpos.x, normalizedScreenpos.y, -1., 1.) + .5;
    clamp_texcoord(scrpos.xy);

    float theta = scrpos.y * PI;
    float phi = PI + ln + scrpos.x * TWOPI;
    while (phi > TWOPI)
        phi -= TWOPI;

    vec3 xcart;
    xcart.x = sin(theta) * cos(phi);
    xcart.y = sin(theta) * sin(phi);
    xcart.z = cos(theta);
    /*
    mat3 crot = mat3(
         1.,  0.,         0.,
         0.,  cos(cr), sin(cr),
         0., -sin(cr), cos(cr)
    );
    mat3 rot = mat3(
          cos(lt), 0., sin(lt),
          0.,      1., 0.,
         -sin(lt), 0., cos(lt)
    );
    mat3 crotm = crot * rot;
    */
    mat3 crotm = mat3(
                  cos(lt),       0.,           sin(lt),
       -sin(cr) * sin(lt),  cos(cr), sin(cr) * cos(lt),
       -cos(cr) * sin(lt), -sin(cr), cos(cr) * cos(lt)
    );
    vec3 xcartrot = crotm * xcart;
    if (xcartrot.x < 0.)
        discard;
    texcoord = vec2(rect.w * (xcartrot.y - rect.x), rect.w * (xcartrot.z - rect.y));
    clamp_texcoord(texcoord);

//    float f = xcartrot.z * xcartrot.z + xcartrot.y * xcartrot.y;
//    radius = f * f;
    radius = 1.;
}

void main(void) {
    if (cutOffRadius.x > 1.)
        discard;
    vec4 color;
    vec2 texcoord;
    float texcoord_radius;
    get_lati_texcoord(hgln, hglt, crota, rect, texcoord, texcoord_radius);
    if (isdifference == NODIFFERENCE) {
        color = getColor(texcoord, texcoord, texcoord_radius);
    } else {
        vec2 difftexcoord;
        float difftexcoord_radius;
        get_lati_texcoord(hglnDiff, hgltDiff, crotaDiff, differencerect, difftexcoord, difftexcoord_radius);
        color = getColor(texcoord, difftexcoord, texcoord_radius);
    }
    gl_FragColor = color;
}
