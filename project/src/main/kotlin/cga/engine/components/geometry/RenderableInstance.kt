package cga.engine.components.geometry

import cga.engine.components.shader.ShaderProgram
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL33
import kotlin.math.sin
import kotlin.random.Random

class RenderableInstance(private var base : Renderable,
                         private var offsets : Array<Vector3f> = arrayOf(Vector3f(0f)),
                         private var instanceCount : Int = 1,
                         var _modelMatrix : Matrix4f = Matrix4f(),
                         var _parent : Transformable? = null,
                         var bMoveWithWind: Boolean = false) : Transformable(_modelMatrix, _parent), IRenderable {

    private var instanceDataVBO = IntArray(base.MeshList.size)
    private val vboSize = instanceCount * 16
    private var floatArray = FloatArray(vboSize);
    private var meshAttribs = base.MeshList[0].attribCount;
    val instances = mutableListOf<Transformable>()
    var customUpdateFunction = {}
    private var wind = 0f
    private var currentVelocity = Vector3f()




    init {
        this.getLocalModelMatrix().get(floatArray);
        initialize()
        updateMatrices()
    }
    override fun render(shaderProgram: ShaderProgram) {
        shaderProgram.setUniform("model_matrix", getWorldModelMatrix(), false);
        for(mesh in base.MeshList) {
            if(mesh.bHasTransparency) GL30.glDisable(GL30.GL_CULL_FACE);
            mesh.renderInstanced(shaderProgram, instanceCount)
            GL11.glEnable(GL11.GL_CULL_FACE)
            GL11.glCullFace(GL11.GL_BACK)
        };
    }

    override fun destroy() {
        for(mesh in base.MeshList) mesh.cleanup()
    }

    override fun update(dt: Float, t: Float) {
        wind = 0.0005f * sin(2*t); //println(wind)
        if (bMoveWithWind) updateMatrices { transformable -> transformable.shear(Vector3f(wind, 0f, 0f)) }
    }

    private fun updateMatrices(){
        val list = arrayListOf<Float>();
        for(i in 0 until instanceCount) {
            val t = Transformable(this.getLocalModelMatrix())
            updateFunction(t, i)
            instances.add(t)
            val mat = Matrix4f(t.getLocalModelMatrix())
            list.add(mat.m00());
            list.add(mat.m01());
            list.add(mat.m02());
            list.add(mat.m03());
            list.add(mat.m10());
            list.add(mat.m11());
            list.add(mat.m12());
            list.add(mat.m13());
            list.add(mat.m20());
            list.add(mat.m21());
            list.add(mat.m22());
            list.add(mat.m23());
            list.add(mat.m30());
            list.add(mat.m31());
            list.add(mat.m32());
            list.add(mat.m33());
        }
        floatArray = list.toFloatArray();
        for(vbo in instanceDataVBO) {
            GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, vbo)
            GL30.glBufferData(GL30.GL_ARRAY_BUFFER, floatArray, GL30.GL_STREAM_DRAW)
            GL33.glBufferSubData(GL30.GL_ARRAY_BUFFER, 0, floatArray);
            GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, 0)
        }

    }

    private fun updateMatrices(function: (Transformable) -> Unit){
        val list = arrayListOf<Float>();
        for(i in 0 until instanceCount) {
            val t = instances[i]
            function(t)
            //instances.add(t)
            val mat = Matrix4f(t.getLocalModelMatrix())
            list.add(mat.m00());
            list.add(mat.m01());
            list.add(mat.m02());
            list.add(mat.m03());
            list.add(mat.m10());
            list.add(mat.m11());
            list.add(mat.m12());
            list.add(mat.m13());
            list.add(mat.m20());
            list.add(mat.m21());
            list.add(mat.m22());
            list.add(mat.m23());
            list.add(mat.m30());
            list.add(mat.m31());
            list.add(mat.m32());
            list.add(mat.m33());
        }
        floatArray = list.toFloatArray();
        for(vbo in instanceDataVBO) {
            GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, vbo)
            GL30.glBufferData(GL30.GL_ARRAY_BUFFER, floatArray, GL30.GL_STREAM_DRAW)
            GL33.glBufferSubData(GL30.GL_ARRAY_BUFFER, 0, floatArray);
            GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, 0)
        }

    }

    private fun updateFunction(t: Transformable, index: Int) {
        if (offsets.lastIndex >= index) {
            t.translateLocal(offsets[index]);
            t.rotateLocal(0f, Random.nextDouble(Math.toRadians(180.0)).toFloat(), 0f)
        }

    }

    private fun bindVBO(vboIdx : Int) {

        instanceDataVBO[vboIdx] = GL30.glGenBuffers()
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, instanceDataVBO[vboIdx])
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, floatArray, GL30.GL_STREAM_DRAW)
        GL30.glEnableVertexAttribArray(0 + meshAttribs)
        GL30.glEnableVertexAttribArray(1 + meshAttribs)
        GL30.glEnableVertexAttribArray(2 + meshAttribs)
        GL30.glEnableVertexAttribArray(3 + meshAttribs)

        GL30.glVertexAttribPointer(0 + meshAttribs, 4, GL30.GL_FLOAT, false, 64, 0)
        GL30.glVertexAttribPointer(1 + meshAttribs, 4, GL30.GL_FLOAT, false, 64, 16)
        GL30.glVertexAttribPointer(2 + meshAttribs, 4, GL30.GL_FLOAT, false, 64, 32)
        GL30.glVertexAttribPointer(3 + meshAttribs, 4, GL30.GL_FLOAT, false, 64, 48)

        GL33.glVertexAttribDivisor(0 + meshAttribs, 1)
        GL33.glVertexAttribDivisor(1 + meshAttribs, 1)
        GL33.glVertexAttribDivisor(2 + meshAttribs, 1)
        GL33.glVertexAttribDivisor(3 + meshAttribs, 1)

        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, 0)
    }
    private fun initialize() {
        for (mesh in base.MeshList) {
            mesh.bindVAO()
            bindVBO(base.MeshList.indexOf(mesh));
            mesh.unbindVAO()
        }
    }
}