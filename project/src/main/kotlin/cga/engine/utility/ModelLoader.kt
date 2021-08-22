package cga.engine.utility

import cga.engine.components.animation.*
import cga.engine.components.geometry.Material
import cga.engine.components.geometry.Mesh
import cga.engine.components.geometry.Renderable
import cga.engine.components.geometry.VertexAttribute
import cga.engine.components.texture.Texture2D
import org.joml.*
import org.lwjgl.BufferUtils
import org.lwjgl.PointerBuffer
import org.lwjgl.assimp.*
import org.lwjgl.opengl.GL11
import java.lang.Integer.max
import java.nio.IntBuffer
import java.util.*

data class BoneInfo(var id: Int = -1, var offsetMatrix: Matrix4f = Matrix4f())

object ModelLoader {
    private val animationList = mutableListOf<Animation>()
    var rootMatrix = Matrix4f()
    private fun load(objPath: String): RawModel? {
        val rm = RawModel()
        val mBoneMap = mutableMapOf<String, BoneInfo>()
        try {
            val aiScene = Assimp.aiImportFile(objPath, Assimp.aiProcess_Triangulate or
                    Assimp.aiProcess_GenNormals or
                    Assimp.aiProcess_CalcTangentSpace or
                    Assimp.aiProcess_JoinIdenticalVertices or
                    Assimp.aiProcess_LimitBoneWeights
            )
                    ?: return null

            // Fix for Collada Root Matrix

            val root = aiScene.mRootNode()?.mTransformation()
            rootMatrix = root?.let { toMatrix4f(it) } ?: Matrix4f()
            val xRow = Vector4f(root?.a1() ?: 0f, root?.a2() ?: 0f, root?.a3() ?: 0f, root?.a4() ?: 0f)
            val yRow = Vector4f(root?.b1() ?: 0f, root?.b2() ?: 0f, root?.b3() ?: 0f, root?.b4() ?: 0f)
            val zRow= Vector4f(root?.c1() ?: 0f, root?.c2() ?: 0f, root?.c3() ?: 0f, root?.c4() ?: 0f)
            val wRow = Vector4f(root?.d1() ?: 0f, root?.d2() ?: 0f, root?.d3() ?: 0f, root?.d4() ?: 0f)
            /*if (yRow.y != 1f) {
                aiScene.mRootNode()?.mTransformation()?.set(
                    xRow.x, xRow.y, xRow.z, xRow.w,
                    zRow.x, zRow.y, zRow.z, zRow.w,
                    yRow.x, yRow.y, yRow.z, yRow.w,
                    wRow.x, wRow.y, wRow.z, wRow.w)
            }*/


            // read materials
            for (m in 0 until aiScene.mNumMaterials()) {
                val rmat = RawMaterial()
                val tpath = AIString.calloc()
                val sceneMat = aiScene.mMaterials() ?: return null
                val mat = AIMaterial.create(sceneMat[m])
                Assimp.aiGetMaterialTexture(mat, Assimp.aiTextureType_DIFFUSE, 0, tpath, null as IntBuffer?, null, null, null, null, null)
                // diffuse texture
                var tpathj = tpath.dataString()
                if (rm.textures.contains(tpathj)) rmat.diffTexIndex = rm.textures.indexOf(tpathj) else {
                    rm.textures.add(tpathj)
                    rmat.diffTexIndex = rm.textures.size - 1
                }
                // specular texture
                Assimp.aiGetMaterialTexture(mat, Assimp.aiTextureType_SPECULAR, 0, tpath, null as IntBuffer?, null, null, null, null, null)
                tpathj = tpath.dataString()
                if (rm.textures.contains(tpathj)) rmat.specTexIndex = rm.textures.indexOf(tpathj) else {
                    rm.textures.add(tpathj)
                    rmat.specTexIndex = rm.textures.size - 1
                }
                // emissive texture
                Assimp.aiGetMaterialTexture(mat, Assimp.aiTextureType_EMISSIVE, 0, tpath, null as IntBuffer?, null, null, null, null, null)
                tpathj = tpath.dataString()
                if (rm.textures.contains(tpathj)) rmat.emitTexIndex = rm.textures.indexOf(tpathj) else {
                    rm.textures.add(tpathj)
                    rmat.emitTexIndex = rm.textures.size - 1
                }
                // shininess
                val sptr = PointerBuffer.allocateDirect(1)
                Assimp.aiGetMaterialProperty(mat, Assimp.AI_MATKEY_SHININESS, sptr)
                val sprop = AIMaterialProperty.create(sptr[0])
                rmat.shininess = sprop.mData().getFloat(0)
                rm.materials.add(rmat)
            }
            // read meshes
            val meshes = ArrayList<RawMesh>()
            for (m in 0 until aiScene.mNumMeshes()) {
                val sceneMeshes = aiScene.mMeshes() ?: return null
                val aiMesh = AIMesh.create(sceneMeshes[m])
                val mesh = RawMesh()

                // read vertices
                for (v in 0 until aiMesh.mNumVertices()) {
                    val aiVert = aiMesh.mVertices()[v]
                    val sceneNormals = aiMesh.mNormals() ?: return null
                    val aiNormal = sceneNormals[v]
                    val sceneTangents = aiMesh.mTangents() ?: return null
                    val aiTangent = sceneTangents[v]
                    val sceneBitangents = aiMesh.mBitangents() ?: return null
                    val aiBitangent = sceneBitangents[v]
                    val sceneTextureCoords = aiMesh.mTextureCoords(0) ?: return null
                    val aiTexCoord = if (aiMesh.mNumUVComponents(0) > 0) sceneTextureCoords[v] else null
                    /*val vert = Vertex(
                            Vector3f(aiVert.x(), aiVert.y(), aiVert.z()),
                            if (aiTexCoord != null) Vector2f(aiTexCoord.x(), aiTexCoord.y()) else Vector2f(0.0f, 0.0f),
                            Vector3f(aiNormal.x(), aiNormal.y(), aiNormal.z())
                    )*/
                    //if (v == 0) println("${aiTangent.x()} ${aiTangent.y()} ${aiTangent.z()}")

                    val vert2 = Vertex(
                        Vector3f(aiVert.x(), aiVert.y(), aiVert.z()),
                        if (aiTexCoord != null) Vector2f(aiTexCoord.x(), aiTexCoord.y()) else Vector2f(0.0f, 0.0f),
                        Vector3f(aiNormal.x(), aiNormal.y(), aiNormal.z()),
                        Vector3f(aiTangent.x(), aiTangent.y(), aiTangent.z()),
                        Vector3f(aiBitangent.x(), aiBitangent.y(), aiBitangent.z())
                    )
                    //println(vert2.tangent)
                    //mesh.vertices.add(vert)
                    mesh.vertices.add(vert2)
                }

                //println("${mesh.vertices.size}")
                for(b in 0 until aiMesh.mNumBones()) {
                    val sceneBones = aiMesh.mBones() ?: break
                    val aiBone = AIBone.create(sceneBones[b])
                    val boneName = aiBone.mName().dataString()
                    val boneWeights = mutableListOf<VertexWeight>()
                    val boneOffsetMatrix = toMatrix4f(aiBone.mOffsetMatrix())
                    //println(boneOffsetMatrix)
                    var boneId: Int
                    val aiWeights = aiBone.mWeights()
                    //println("ID: $b; name: $boneName")
                    if(!mBoneMap.containsKey(boneName)) {
                        val newBoneInfo = BoneInfo(b, boneOffsetMatrix)
                        mBoneMap[boneName] = newBoneInfo
                        boneId = b
                        //println("$boneName == $boneId")
                    }
                    else {
                        boneId = mBoneMap[boneName]?.id!!
                    }

                    assert(boneId != -1)
                    for(w in 0 until aiBone.mNumWeights()) {
                        val aiWeight = aiWeights[w]
                        val vertexWeight = VertexWeight(aiWeight.mVertexId(), aiWeight.mWeight())

                        assert(vertexWeight.vertexID < mesh.vertices.size)
                        setVertexBoneWeights(mesh.vertices[vertexWeight.vertexID], boneId, vertexWeight.weight)
                    }

                }

                // read indices
                for (f in 0 until aiMesh.mNumFaces()) {
                    val face = aiMesh.mFaces()[f]
                    for (i in 0 until face.mNumIndices()) {
                        mesh.indices.add(face.mIndices()[i])
                    }
                }


                // material index
                mesh.materialIndex = aiMesh.mMaterialIndex()
                meshes.add(mesh)
            }

            val boneMesh = RawMesh()
            var rootBone: Bone? = null


            // traverse assimp scene graph
            val nodeQueue: Queue<AINode> = LinkedList()
            val children = LinkedList<String>()

            val rootTransform = aiScene.mRootNode()?.mTransformation()?.let { toMatrix4f(it) }
            if ( rootTransform != null) {
                val name = aiScene.mRootNode()?.mName()?.dataString()
                //println("$name\n$rootTransform")
            }
            else {
                println("root is null")
            }
            nodeQueue.offer(aiScene.mRootNode())

            while (!nodeQueue.isEmpty()) {
                val node = nodeQueue.poll()
                if (rootBone == null ) rootBone = constructBoneHierarchy(node, mBoneMap, null)
                children.clear()
                for (m in 0 until node.mNumMeshes()) {
                    var sceneMeshes = node.mMeshes() ?: return null
                    //rm.meshes.add(mesh)
                    rm.meshes.add(meshes[sceneMeshes[m]])
                }
                for (c in 0 until node.mNumChildren()) {
                    val sceneChildren = node.mChildren() ?: return null
                    val cnode = AINode.create(sceneChildren[c])
                    children.add(cnode.mName().dataString())
                    nodeQueue.offer(cnode)
                }

            }

            //rootBone?.print()

            for (mesh in rm.meshes) {
                mesh.rootBone = rootBone
                mesh.boneCount = 15

            }



            val sceneAnims = aiScene.mAnimations()

            sceneAnims?.let {
                for(a in 0 until aiScene.mNumAnimations()) {
                    val anim = AIAnimation.create(sceneAnims[a]) ?: break
                    val keyframes = mutableListOf<KeyFrame>()
                    val animNodes = mutableListOf<AnimNode>()
                    val timestamps = mutableListOf<Float>()
                    var maxKeyframes = 0

                    //println("animation")
                    val poseList = mutableListOf<Map<String, BoneTransform>>()


                    anim.mChannels()?.let { keyframe ->
                        for (n in 0 until anim.mNumChannels()) {
                            val nodeAnim = AINodeAnim.create(keyframe[n])

                            val boneName = nodeAnim.mNodeName().dataString()
                            val numKeyframes = max(max(nodeAnim.mNumPositionKeys(), nodeAnim.mNumRotationKeys()), nodeAnim.mNumScalingKeys())
                            maxKeyframes = max(maxKeyframes, numKeyframes)

                            var pos = mutableListOf<Vector3f>(); var rot = mutableListOf<Quaternionf>(); var scl = mutableListOf<Vector3f>()
                            for (p in 0 until nodeAnim.mNumPositionKeys()) {
                                val pk = nodeAnim.mPositionKeys()?.get(p) ?: break
                                pos.add(toVector3f(pk.mValue()))
                                val timestamp = pk.mTime().toFloat()
                                if(!timestamps.contains(timestamp)) timestamps.add(timestamp)
                                //println("${pk.mTime()}")
                            }

                            for (r in 0 until nodeAnim.mNumRotationKeys()) {
                                val rk = nodeAnim.mRotationKeys()?.get(r) ?: break
                                rot.add(toQuaternionf(rk.mValue()))
                                //println("${rk.mTime()}")
                            }

                            for (s in 0 until nodeAnim.mNumScalingKeys()) {
                                val sk = nodeAnim.mScalingKeys()?.get(s) ?: break
                                scl.add(toVector3f(sk.mValue()))
                                //println("${sk.mTime()}")
                            }


                            val animNode = AnimNode(boneName, pos.toTypedArray(), rot.toTypedArray(), scl.toTypedArray())
                            animNodes.add(animNode)
                        }


                        for (i in 0 until maxKeyframes){
                            val map = hashMapOf<String, BoneTransform>()
                            for (bone in animNodes) {
                                val pos = bone.keyPositions[i]
                                val rot = bone.keyRotations[i]
                                val scl = bone.keyScales[i]
                                map[bone.name] = BoneTransform(pos, rot, scl)
                            }
                            poseList.add(map)
                        }

                        for(p in poseList) {
                            //println("$p")
                            val keyFrame = KeyFrame(timestamps[poseList.indexOf(p)], p)
                            keyframes.add(keyFrame)
                        }
                    }
                    val animDuration = (anim.mDuration() / anim.mTicksPerSecond()).toFloat()
                    val newAnimation = Animation(animDuration, keyframes.toTypedArray())
                    animationList.add(newAnimation)
                }
            }


        } catch (ex: Exception) {
            throw Exception("Something went terribly wrong. Thanks java.\n" + ex.message)
        }
        return rm
    }

    private fun flattenVertexData(vertices: List<Vertex>, rot: Matrix3f): FloatArray {
        //val data = FloatArray(8 * vertices.size)
        var di = 0
        /*for ((position, texCoord, normal) in vertices) {
            position.mul(rot)
            normal.mul(Matrix3f(rot).transpose().invert())
            data[di++] = position.x
            data[di++] = position.y
            data[di++] = position.z
            data[di++] = texCoord.x
            data[di++] = texCoord.y
            data[di++] = normal.x
            data[di++] = normal.y
            data[di++] = normal.z
        }*/
        val data = FloatArray(14 * vertices.size)
        for ((position, texCoord, normal, tangent, bitangent) in vertices) {
            position.mul(rot)
            normal.mul(Matrix3f(rot).transpose().invert())
            tangent.mul(Matrix3f(rot).transpose().invert())
            bitangent.mul(Matrix3f(rot).transpose().invert())
            data[di++] = position.x
            data[di++] = position.y
            data[di++] = position.z
            data[di++] = texCoord.x
            data[di++] = texCoord.y
            data[di++] = normal.x
            data[di++] = normal.y
            data[di++] = normal.z
            data[di++] = tangent.x
            data[di++] = tangent.y
            data[di++] = tangent.z
            data[di++] = bitangent.x
            data[di++] = bitangent.y
            data[di++] = bitangent.z
        }

        //println(vertices.size)
        /*val data = FloatArray(22 * vertices.size)
        for ((position, texCoord, normal, tangent, bitangent, boneIDs, boneWeights) in vertices) {
            position.mul(rot)
            normal.mul(Matrix3f(rot).transpose().invert())
            tangent.mul(Matrix3f(rot).transpose().invert())
            bitangent.mul(Matrix3f(rot).transpose().invert())
            data[di++] = position.x
            data[di++] = position.y
            data[di++] = position.z
            data[di++] = texCoord.x
            data[di++] = texCoord.y
            data[di++] = normal.x
            data[di++] = normal.y
            data[di++] = normal.z
            data[di++] = tangent.x
            data[di++] = tangent.y
            data[di++] = tangent.z
            data[di++] = bitangent.x
            data[di++] = bitangent.y
            data[di++] = bitangent.z
            data[di++] = boneIDs.x.toFloat()
            data[di++] = boneIDs.y.toFloat()
            data[di++] = boneIDs.z.toFloat()
            data[di++] = boneIDs.w.toFloat()
            data[di++] = boneWeights.x
            data[di++] = boneWeights.y
            data[di++] = boneWeights.z
            data[di++] = boneWeights.w
        }*/
        return data
    }

    private fun flattenIndexData(indices: List<Int>): IntArray {
        val data = IntArray(indices.size)
        var di = 0
        for (i in indices) {
            data[di++] = i
        }
        return data
    }

    private fun flattenBoneIDsData(vertices: List<Vertex>) : IntArray {
        val data = IntArray(4 * vertices.size)
        var counter = 0
        for ((_, _, _, _, _, boneIDs) in vertices) {
            data[counter++] = boneIDs.x()
            data[counter++] = boneIDs.y()
            data[counter++] = boneIDs.z()
            data[counter++] = boneIDs.w()
            //println("$boneIDs")
        }
        return data
    }

    private fun flattenWeightsData(vertices: List<Vertex>) : FloatArray {
        val data = FloatArray(4 * vertices.size)
        var counter = 0
        for ((_, _, _, _, _, _, weight) in vertices) {
            data[counter++] = weight.x()
            data[counter++] = weight.y()
            data[counter++] = weight.z()
            data[counter++] = weight.w()
            //println("$weight")
        }
        return data
    }

    fun loadModel(objpath: String, pitch: Float, yaw: Float, roll: Float): Renderable? {
        val model = load(objpath) ?: return null
        val textures = ArrayList<Texture2D>()
        val texturePaths = ArrayList<String>()
        val materials = ArrayList<Material>()
        val meshes = ArrayList<Mesh>()
        val stride = 8 * 4
        val stride2 = 14 * 4
        val stride3 = 22 * 4
        /*val atr1 = VertexAttribute(3, GL11.GL_FLOAT, stride, 0)
        val atr2 = VertexAttribute(2, GL11.GL_FLOAT, stride, 3 * 4)
        val atr3 = VertexAttribute(3, GL11.GL_FLOAT, stride, 5 * 4)*/

        val atr1 = VertexAttribute(3, GL11.GL_FLOAT, stride2, 0)
        val atr2 = VertexAttribute(2, GL11.GL_FLOAT, stride2, 3 * 4)
        val atr3 = VertexAttribute(3, GL11.GL_FLOAT, stride2, 5 * 4)
        val atr4 = VertexAttribute(3, GL11.GL_FLOAT, stride2, 8 * 4)
        val atr5 = VertexAttribute(3, GL11.GL_FLOAT, stride2, 11 * 4)


        /*val atr1 = VertexAttribute(3, GL11.GL_FLOAT, stride3, 0)
        val atr2 = VertexAttribute(2, GL11.GL_FLOAT, stride3, 3 * 4)
        val atr3 = VertexAttribute(3, GL11.GL_FLOAT, stride3, 5 * 4)
        val atr4 = VertexAttribute(3, GL11.GL_FLOAT, stride3, 8 * 4)
        val atr5 = VertexAttribute(3, GL11.GL_FLOAT, stride3, 11 * 4)
        val atr6 = VertexAttribute(4, GL11.GL_INT, stride3, 14 * 4)
        val atr7 = VertexAttribute(4, GL11.GL_FLOAT, stride3, 18 * 4)*/

        //val vertexAttributes = arrayOf(atr1, atr2, atr3)
        val vertexAttributes2 = arrayOf(atr1, atr2, atr3, atr4, atr5)
        //val vertexAttributes3 = arrayOf(atr1, atr2, atr3, atr4, atr5, atr6, atr7)
        // preprocessing rotation
        val rot = Matrix3f().rotateZ(roll).rotateY(yaw).rotateX(pitch)
        // create textures
        //default textures
        val ddata = BufferUtils.createByteBuffer(4)
        ddata.put(0.toByte()).put(0.toByte()).put(0.toByte()).put(0.toByte())
        ddata.flip()
        for (i in model.textures.indices) {
            if (model.textures[i].isEmpty()) {
                texturePaths.add("")
                textures.add(Texture2D(ddata, 1, 1, genMipMaps = true, isSRGB = false))
            } else {
                texturePaths.add(objpath.substring(0, objpath.lastIndexOf('/') + 1) + model.textures[i])
                textures.add(Texture2D(objpath.substring(0, objpath.lastIndexOf('/') + 1) + model.textures[i], true))
            }
        }
        // materials
        for (i in model.materials.indices) {
            val diff = if(texturePaths[model.materials[i].diffTexIndex] == "") textures[model.materials[i].diffTexIndex]
                            else { Texture2D(texturePaths[model.materials[i].diffTexIndex], genMipMaps = true, isSRGB = true) }
            /*materials.add(Material(textures[model.materials[i].diffTexIndex],
                    textures[model.materials[i].emitTexIndex],
                    textures[model.materials[i].specTexIndex],
                    model.materials[i].shininess, Vector2f(1.0f, 1.0f)))*/
            materials.add(Material(diff,
                textures[model.materials[i].emitTexIndex],
                textures[model.materials[i].specTexIndex],
                model.materials[i].shininess, Vector2f(1.0f, 1.0f)))
        }
        // meshes
        /*for (i in model.meshes.indices) {
            meshes.add(Mesh(flattenVertexData(model.meshes[i].vertices, rot),
                    flattenIndexData(model.meshes[i].indices),
                    vertexAttributes2,
                    materials[model.meshes[i].materialIndex]))
        }*/
        for (i in model.meshes.indices) {
            val mesh = Mesh(
                flattenVertexData(model.meshes[i].vertices, rot),
                flattenIndexData(model.meshes[i].indices),
                vertexAttributes2,
                materials[model.meshes[i].materialIndex],
                model.meshes[i].rootBone,
                model.meshes[i].boneCount
            )

            mesh.setAnimated(true, flattenBoneIDsData(model.meshes[i].vertices), flattenWeightsData(model.meshes[i].vertices))

            meshes.add(mesh)
        }
        // assemble the renderable
        val output = Renderable(meshes)
        output.addAnimation(animationList)

        return output
    }

    private fun toVector3f(aiVector3D: AIVector3D): Vector3f = Vector3f(aiVector3D.x(), aiVector3D.y(), aiVector3D.z())
    private fun toQuaternionf(aiQuaternion: AIQuaternion): Quaternionf = Quaternionf(aiQuaternion.x(), aiQuaternion.y(), aiQuaternion.z(), aiQuaternion.w())

    private fun toMatrix4f(aiMatrix4x4: AIMatrix4x4): Matrix4f {
        val result = Matrix4f()
        result.m00(aiMatrix4x4.a1())
        result.m10(aiMatrix4x4.a2())
        result.m20(aiMatrix4x4.a3())
        result.m30(aiMatrix4x4.a4())
        result.m01(aiMatrix4x4.b1())
        result.m11(aiMatrix4x4.b2())
        result.m21(aiMatrix4x4.b3())
        result.m31(aiMatrix4x4.b4())
        result.m02(aiMatrix4x4.c1())
        result.m12(aiMatrix4x4.c2())
        result.m22(aiMatrix4x4.c3())
        result.m32(aiMatrix4x4.c4())
        result.m03(aiMatrix4x4.d1())
        result.m13(aiMatrix4x4.d2())
        result.m23(aiMatrix4x4.d3())
        result.m33(aiMatrix4x4.d4())
        return result
    }


    private fun setVertexBoneWeights(vertex: Vertex, boneID: Int, weight: Float) {
        if(vertex.boneIDs.x == -1) {
            vertex.boneIDs.x = boneID
            vertex.boneWeights.x = weight
        }
        else if(vertex.boneIDs.y == -1) {
            vertex.boneIDs.y = boneID
            vertex.boneWeights.y = weight
        }
        else if(vertex.boneIDs.z == -1) {
            vertex.boneIDs.z = boneID
            vertex.boneWeights.z = weight
        }
        else if(vertex.boneIDs.w == -1) {
            vertex.boneIDs.w = boneID
            vertex.boneWeights.w = weight
        }
    }

    private fun constructBoneHierarchy(node: AINode, boneMap: Map<String, BoneInfo>, parentBone: Bone? = null) : Bone? {
        val nodeName = node.mName().dataString()
        val parentTransform = Matrix4f(parentBone?.localTransform ?: Matrix4f())
        //println(parentTransform)
        val localTransform = toMatrix4f(node.mTransformation())
        val globalTransform =  parentTransform.mul(localTransform)
        //println("$nodeName\n$globalTransform")

        var bone : Bone? = null
        var boneCount = 0

        boneMap[nodeName]?.let {

            bone = Bone(it.id, nodeName, Matrix4f(it.offsetMatrix), localTransform, globalTransform)
            boneCount += 1

            if(node.mNumChildren() > 0) {
                for (c in 0 until node.mNumChildren()) {
                    val children = node.mChildren() ?: break
                    val child = AINode.create(children.get(c))
                    val boneChild = constructBoneHierarchy(child, boneMap, bone)
                    if (boneChild != null) {
                        bone?.addChild(boneChild)
                    }
                }
            }
        }
        return bone
    }



    fun loadAnimation(path: String, renderable: Renderable) {
        try {
            val model = load(path) ?: throw Exception("Error loading the model")
            renderable.animations.add(animationList.last())

        } catch (ex: Exception) {
            throw Exception("Error loading the fucking animation\n" + ex.message)
        }


    }

    private fun checkBone(boneName: String, renderable: Renderable): Boolean {
        for (mesh in renderable.MeshList) {
            val bone = mesh.rootBone ?: return false
            if (!(isSameBone(bone, boneName))) return false
        }
        return true
    }

    private fun isSameBone(bone: Bone, boneName: String) : Boolean {
        var result =  boneName != bone.name
        for (child in bone.children) result = result && isSameBone(child, boneName)
        return result
    }

}