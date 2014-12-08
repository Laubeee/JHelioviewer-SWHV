package org.helioviewer.viewmodel.view.opengl;

import javax.media.opengl.GL2;

import org.helioviewer.base.physics.Constants;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.viewmodel.metadata.HelioviewerOcculterMetaData;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.view.HelioviewerGeometryView;
import org.helioviewer.viewmodel.view.MetaDataView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderView;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder.GLBuildShaderException;
import org.helioviewer.viewmodel.view.opengl.shader.GLTextureCoordinate;
import org.helioviewer.viewmodel.view.opengl.shader.GLVertexShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLVertexShaderView;

/**
 * Implementation of HelioviewGeometryView for rendering in OpenGL mode.
 *
 * <p>
 * This class provides vertex- and fragment shader blocks to cut away invalid
 * parts of solar images. It does so by calculating the distance from the center
 * for every single pixel on the screen. If the distance is outside the valid
 * area of that specific image, its alpha value is set to zero, otherwise it
 * remains untouched.
 *
 * <p>
 * For further information about the role of the HelioviewerGeometryView within
 * the view chain, see
 * {@link org.helioviewer.viewmodel.view.HelioviewerGeometryView}
 *
 * @author Markus Langenberg
 */
public class GLHelioviewerGeometryView extends AbstractGLView implements HelioviewerGeometryView, GLFragmentShaderView, GLVertexShaderView {

    GeometryVertexShaderProgram vertexShader = new GeometryVertexShaderProgram();
    GeometryFragmentShaderProgram fragmentShader = new GeometryFragmentShaderProgram();
    private final boolean test;

    public GLHelioviewerGeometryView() {
        this.test = true;
    }

    /**
     * {@inheritDoc}
     *
     * In this case, does nothing.
     */
    @Override
    protected void setViewSpecificImplementation(View newView, ChangeEvent changeEvent) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void renderGL(GL2 gl, boolean nextView) {
        gl.glEnable(GL2.GL_FRAGMENT_PROGRAM_ARB);
        gl.glEnable(GL2.GL_VERTEX_PROGRAM_ARB);

        vertexShader.bind(gl);
        fragmentShader.bind(gl);

        MetaData metaData = getAdapter(MetaDataView.class).getMetaData();
        if (metaData instanceof HelioviewerOcculterMetaData) {
            fragmentShader.setMaskRotation(gl, (float) ((HelioviewerOcculterMetaData) metaData).getMaskRotation());
        }

        renderChild(gl);

        gl.glDisable(GL2.GL_FRAGMENT_PROGRAM_ARB);
        gl.glDisable(GL2.GL_VERTEX_PROGRAM_ARB);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GLShaderBuilder buildFragmentShader(GLShaderBuilder shaderBuilder) {
        GLFragmentShaderView nextView = view.getAdapter(GLFragmentShaderView.class);
        if (nextView != null) {
            shaderBuilder = nextView.buildFragmentShader(shaderBuilder);
        }

        fragmentShader.build(shaderBuilder);
        return shaderBuilder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GLShaderBuilder buildVertexShader(GLShaderBuilder shaderBuilder) {
        GLVertexShaderView nextView = view.getAdapter(GLVertexShaderView.class);
        if (nextView != null) {
            shaderBuilder = nextView.buildVertexShader(shaderBuilder);
        }

        vertexShader.build(shaderBuilder);
        return shaderBuilder;
    }

    /**
     * Private class representing a fragment shader block capable to cut out
     * invalid parts of solar images.
     *
     * <p>
     * Since branching (=using if statements) is not supported on most graphics
     * cards, the decision whether to set the alpha value to zero or leave it
     * untouched is achieved by using the step function (x < 0 ? 0 : 1). For
     * disc images, the current alpha value of the pixel is multiplied with a
     * shifted and mirrored step function. For occulter images, a shifted and a
     * shifted and mirrored step function are used.
     *
     * <p>
     * The physical position is provides in the third texture coordinate by the
     * {@link GeometryVertexShaderProgram}.
     *
     * <p>
     * For further information about how to build shaders, see
     * {@link org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder} as
     * well as the Cg User Manual.
     */
    private class GeometryFragmentShaderProgram extends GLFragmentShaderProgram {
        private GLTextureCoordinate rotationParam;

        private void setMaskRotation(GL2 gl, float maskRotation) {
            if (rotationParam != null) {
                rotationParam.setValue(gl, maskRotation);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void buildImpl(GLShaderBuilder shaderBuilder) {

            MetaData metaData = view.getAdapter(MetaDataView.class).getMetaData();

            if (metaData instanceof HelioviewerOcculterMetaData) {
                // LASCO

                HelioviewerOcculterMetaData hvMetaData = (HelioviewerOcculterMetaData) metaData;

                try {
                    rotationParam = shaderBuilder.addTexCoordParameter(1);
                    String program = "\tfloat geometryRadius = length(physicalPosition.zw);" + GLShaderBuilder.LINE_SEP;
                    program += "\tfloat2x2 geometryFlatDistRotationMatrix = float2x2(cos(maskRotation), sin(maskRotation), -sin(maskRotation), cos(maskRotation));" + GLShaderBuilder.LINE_SEP;
                    program += "\tfloat2 geometryFlatDist = abs(mul(geometryFlatDistRotationMatrix, physicalPosition.zw));" + GLShaderBuilder.LINE_SEP;
                    program += "\toutput.a = output.a * step(innerRadius, geometryRadius) * step(-outerRadius, -geometryRadius) * step(-flatDist, -geometryFlatDist.x) * step(-flatDist, -geometryFlatDist.y);";

                    program = program.replace("output", shaderBuilder.useOutputValue("float4", "COLOR"));
                    program = program.replace("physicalPosition", shaderBuilder.useStandardParameter("float4", "TEXCOORD0"));
                    program = program.replace("maskRotation", rotationParam.getIdentifier());
                    program = program.replace("innerRadius", Double.toString(hvMetaData.getInnerPhysicalOcculterRadius() * roccInnerFactor).replace(',', '.'));
                    program = program.replace("outerRadius", Double.toString(hvMetaData.getOuterPhysicalOcculterRadius() * roccOuterFactor).replace(',', '.'));
                    program = program.replace("flatDist", Double.toString(hvMetaData.getPhysicalFlatOcculterSize()).replace(',', '.'));
                    if (Displayer.getSingletonInstance().getState() == Displayer.STATE2D) {
                        shaderBuilder.addMainFragment(program);
                    }
                } catch (GLBuildShaderException e) {
                    e.printStackTrace();
                }
            } else if (metaData instanceof HelioviewerMetaData) {

                HelioviewerMetaData hvMetaData = (HelioviewerMetaData) metaData;

                // MDI and HMI
                if (hvMetaData.getInstrument().equalsIgnoreCase("MDI") || hvMetaData.getInstrument().equalsIgnoreCase("HMI")) {

                    try {
                        String program = "\tfloat geometryRadius = -length(physicalPosition.zw);" + GLShaderBuilder.LINE_SEP;
                        program += "\toutput.a  = output.a * step(-sunRadius, geometryRadius);";

                        program = program.replace("output", shaderBuilder.useOutputValue("float4", "COLOR"));
                        program = program.replace("physicalPosition", shaderBuilder.useStandardParameter("float4", "TEXCOORD0"));
                        program = program.replace("sunRadius", Double.toString(Constants.SunRadius * discFactor).replace(',', '.'));

                        if (Displayer.getSingletonInstance().getState() == Displayer.STATE2D) {
                            shaderBuilder.addMainFragment(program);
                        }
                    } catch (GLBuildShaderException e) {
                        e.printStackTrace();
                    }
                } else {
                    // EIT and AIA

                    try {
                        String program = "\tfloat geometryRadius = -length(physicalPosition.zw);" + GLShaderBuilder.LINE_SEP;
                        program += "\tfloat fadeDisc = smoothstep(-fadedSunRadius, -sunRadius, geometryRadius);" + GLShaderBuilder.LINE_SEP;
                        program += "\tfloat maxPixelValue = max(max(output.r, output.g), max(output.b, 0.001));" + GLShaderBuilder.LINE_SEP;
                        program += "\toutput.a = output.a * (fadeDisc + (1-fadeDisc) * pow(maxPixelValue, 1-output.a));";

                        program = program.replace("output", shaderBuilder.useOutputValue("float4", "COLOR"));
                        program = program.replace("physicalPosition", shaderBuilder.useStandardParameter("float4", "TEXCOORD0"));
                        program = program.replace("sunRadius", Double.toString(Constants.SunRadius).replace(',', '.'));
                        program = program.replace("fadedSunRadius", Double.toString(Constants.SunRadius * discFadingFactor).replace(',', '.'));

                        if (Displayer.getSingletonInstance().getState() == Displayer.STATE2D) {
                            shaderBuilder.addMainFragment(program);
                        }
                    } catch (GLBuildShaderException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Private class representing a vertex shader block, providing information
     * necessary cutting out invalid areas of solar images.
     *
     * <p>
     * To decide, whether a pixel belongs to an invalid area or not, it needs
     * the physical position of the pixel. From within the view chain, this is
     * achieved by using drawing the vertices to their physical position. While
     * being processed by the vertex shader, the vertices are moved to their
     * final screen location, so this shader block moves the position to the
     * third texture coordinate before transforming the vertices, to the
     * physical position is still available for fragment shader.
     */
    private class GeometryVertexShaderProgram extends GLVertexShaderProgram {

        /**
         * {@inheritDoc}
         */
        @Override
        protected void buildImpl(GLShaderBuilder shaderBuilder) {
            try {
                String program = "\toutput.zw = physicalPosition.xy;";
                program = program.replace("output", shaderBuilder.useOutputValue("float4", "TEXCOORD0"));
                program = program.replace("physicalPosition", shaderBuilder.useStandardParameter("float4", "POSITION"));
                shaderBuilder.addMainFragment(program);
            } catch (GLBuildShaderException e) {
                e.printStackTrace();
            }
        }
    }

}
