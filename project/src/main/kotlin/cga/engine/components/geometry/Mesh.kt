package cga.engine.components.geometry

import cga.engine.components.animation.Animation
import cga.engine.components.animation.Animator
import cga.engine.components.animation.Bone
import cga.engine.components.shader.ShaderProgram
import org.joml.Matrix4f
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL33

/**
 * Creates a Mesh object from vertexdata, intexdata and a given set of vertex attributes
 *
 * @param vertexdata plain float array of vertex data
 * @param indexdata  index data
 * @param attributes vertex attributes contained in vertex data
 * @throws Exception If the creation of the required OpenGL objects fails, an exception is thrown
 *
 * Created by Fabian on 16.09.2017.
 */
class Mesh(private val vertexdata: FloatArray,
           indexdata: IntArray,
           private var attributes: Array<VertexAttribute>,
           var material: Material? = null,
           var rootBone: Bone? = null,
           var boneCount: Int = 0
) {


    var bDrawLines: Boolean = false

    // Animation Data
    private var boneIdsVbo: Int = 0
    private var boneWeightsVbo: Int =0
    private var boneWeights: FloatArray = floatArrayOf()
    private var boneIDs: IntArray = intArrayOf()
    private var bIsAnimated: Boolean = false
    private var animator: Animator = Animator(this)

    //private data
    private var vao = 0
    private var vbo = 0
    private var ibo = 0
    private var indexcount = 0
    var attribCount = attributes.size + 2;
    private var bInstanced = false;
    var bHasTransparency = false;
    var bUseFakeLighting = false;




    init {
        // todo: place your code here
        indexcount = indexdata.size


        // todo: generate IDs
        vao = GL30.glGenVertexArrays()
        vbo = GL30.glGenBuffers()
        ibo = GL30.glGenBuffers()

        // todo: bind your objects
        GL30.glBindVertexArray(vao)
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, vbo)
        GL30.glBindBuffer(GL30.GL_ELEMENT_ARRAY_BUFFER, ibo)



        // todo: upload your mesh data
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, vertexdata, GL30.GL_DYNAMIC_DRAW)

        for (i in attributes.indices)
        {
            GL30.glEnableVertexAttribArray(i)
            GL30.glVertexAttribPointer(i, attributes[i].n, attributes[i].type, false, attributes[i].stride, attributes[i].offset.toLong())
        }


        GL30.glBufferData(GL30.GL_ELEMENT_ARRAY_BUFFER, indexdata, GL30.GL_DYNAMIC_DRAW)


        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, 0)
        GL30.glBindVertexArray(0)
        GL30.glBindBuffer(GL30.GL_ELEMENT_ARRAY_BUFFER, 0)

    }

    /**
     * renders the mesh
     */
    fun render() {
        // todo: place your code here
        // call the rendering method every frame

        GL30.glBindVertexArray(vao)
        GL30.glDrawElements(GL30.GL_TRIANGLES, indexcount, GL30.GL_UNSIGNED_INT, 0)
        GL30.glBindVertexArray(0)

    }

    fun renderNoMat(shader: ShaderProgram) {
        // todo: place your code here
        // call the rendering method every frame

        shader.setUniform("bIsInstanced", 0);
        GL30.glBindVertexArray(vao)
        GL30.glDrawElements(GL30.GL_TRIANGLES, indexcount, GL30.GL_UNSIGNED_INT, 0)
        GL30.glBindVertexArray(0)

    }

    fun render(shader: ShaderProgram) {
        shader.setUniform("bIsInstanced", 0);
        //shader.setUniform("bUseFakeLighting", bUseFakeLighting);
        bindTransformsArray(shader)
        material?.bind(shader);
        GL30.glBindVertexArray(vao)
        if (bDrawLines) GL30.glDrawElements(GL30.GL_LINE_LOOP, indexcount, GL30.GL_UNSIGNED_INT, 0)
        else GL30.glDrawElements(GL30.GL_TRIANGLES, indexcount, GL30.GL_UNSIGNED_INT, 0)
        GL30.glBindVertexArray(0)

    }

    fun renderInstanced(shader: ShaderProgram, instances : Int) {
        shader.setUniform("bIsInstanced", 1);
        //shader.setUniform("bUseFakeLighting", bUseFakeLighting);
        material?.bind(shader);
        GL30.glBindVertexArray(vao)
        GL33.glDrawElementsInstanced(GL30.GL_TRIANGLES, indexcount, GL30.GL_UNSIGNED_INT, 0, instances)
        GL30.glBindVertexArray(0)
    }

    fun playAnimation(animation: Animation?, playbackSpeed: Float = 1f, loop: Boolean = false) {
        animator.playAnimation(animation, playbackSpeed, loop)
    }

    fun stopAnimation () = animator.stopAnimation()

    fun update(dt: Float) = animator.update(dt)

    /**
     * Deletes the previously allocated OpenGL objects for this mesh
     */
    fun cleanup() {
        if (ibo != 0) GL15.glDeleteBuffers(ibo)
        if (vbo != 0) GL15.glDeleteBuffers(vbo)
        if (vao != 0) GL30.glDeleteVertexArrays(vao)
        material?.cleanup();
    }

    private fun calculateTangents() {
        val positions = FloatArray(vertexdata.size)
        val uvs = FloatArray(vertexdata.size)

        for(i in vertexdata.indices) {

        }


        /*
        var i = 0
        while (i < mesh.indices.size) {
            val v1 = Vector3f(mesh.vertices[mesh.indices[i]].position)
            val v2 = Vector3f(mesh.vertices[mesh.indices[i + 1]].position)
            val v3 = Vector3f(mesh.vertices[mesh.indices[i + 2]].position)
            //counter clockwise winding
            val edge1 = Vector3f()
            v2.sub(v1, edge1)
            val edge2 = Vector3f()
            v3.sub(v1, edge2)
            val normal = Vector3f()
            edge1.cross(edge2, normal)
            //for each Vertex all corresponding normals are added. The result is a non unit length vector which is the average direction of all assigned normals.
            mesh.vertices[mesh.indices[i]].normal.add(normal)
            mesh.vertices[mesh.indices[i + 1]].normal.add(normal)
            mesh.vertices[mesh.indices[i + 2]].normal.add(normal)
            i += 3
        }*/
    }

    fun bindVAO() {GL30.glBindVertexArray(vao)}
    fun unbindVAO() {GL30.glBindVertexArray(0)}
    fun setInstanced(inst : Boolean) {bInstanced = inst;}
    fun getBoneTransforms() :  MutableList<Matrix4f> {
        val boneMatrices = MutableList(boneCount) { Matrix4f() }
        addBonesToList(rootBone, boneMatrices)

        return boneMatrices;
    }

    private fun bindTransformsArray(shader: ShaderProgram) : Boolean {
        var res = true
        val list = getBoneTransforms();
        for ((c, t) in list.withIndex()) {

            val r = (shader.setUniform("boneMatrix$c", t, false))
            (shader.setUniform("boneMatrices[$c]", t, false))
            res = res && r
        }
        return res
    }

    private fun addBonesToList(headBone: Bone?, boneMatrices: MutableList<Matrix4f>) {
        if(headBone != null) {
            boneMatrices[headBone.index] = headBone.getAnimatedTransform()
            for (child in headBone.children) {
                addBonesToList(child, boneMatrices)
            }
        }
    }

    fun setAnimated(bIsAnimated: Boolean, boneIDs: IntArray, boneWeights: FloatArray) {
        if(bIsAnimated) {
            this.bIsAnimated = bIsAnimated
            this.boneIDs = boneIDs
            this.boneWeights = boneWeights
            uploadAnimData()
        }
    }

    private fun uploadAnimData() {
        bindVAO()
        boneIdsVbo = GL30.glGenBuffers()
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, boneIdsVbo)
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, boneIDs, GL30.GL_DYNAMIC_DRAW)
        GL30.glEnableVertexAttribArray(0 + attributes.size)
        GL30.glVertexAttribIPointer(0 + attributes.size, 4, GL30.GL_INT, 16, 0)
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, 0)

        boneWeightsVbo = GL30.glGenBuffers()
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, boneWeightsVbo)
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, boneWeights, GL30.GL_DYNAMIC_DRAW)
        GL30.glEnableVertexAttribArray(1 + attributes.size)
        GL30.glVertexAttribPointer(1 + attributes.size, 4, GL30.GL_FLOAT, false, 16, 0)
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, 0)
        unbindVAO()

    }
}