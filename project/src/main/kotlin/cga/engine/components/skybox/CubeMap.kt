package cga.engine.components.skybox

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30
import org.lwjgl.stb.STBImage
import java.nio.ByteBuffer

class CubeMap(cubeMapData: Array<ByteBuffer>, width: Int, height: Int) {

    private var texID = -1;

    init {
        try {
            processCubeMapTexture(cubeMapData, width, height);
        } catch (e : java.lang.Exception)
        {
            e.printStackTrace()
        }
    }
    companion object
    {
        operator fun invoke(path : String) : CubeMap {
            val x = BufferUtils.createIntBuffer(1);
            val y = BufferUtils.createIntBuffer(1);
            val readChannels = BufferUtils.createIntBuffer(1);
            var faceName = "";
            val faces = arrayListOf<ByteBuffer>();

            STBImage.stbi_set_flip_vertically_on_load(false);
            for(i in 0..5) {
                when(i) {
                    0 -> faceName = "px.png";
                    1 -> faceName = "nx.png";
                    2 -> faceName = "py.png";
                    3 -> faceName = "ny.png";
                    4 -> faceName = "pz.png";
                    5 -> faceName = "nz.png";
                }
                val imageData = STBImage.stbi_load(path + faceName, x, y, readChannels, 4)
                        ?: throw Exception("Image file \"" + path + "\" couldn't be read:\n" + STBImage.stbi_failure_reason())

                faces.add(imageData);
            }

            try {
                return CubeMap(faces.toTypedArray() , x.get(), y.get())
            } catch (ex: java.lang.Exception) {
                ex.printStackTrace()
                throw ex
            } finally {
                for (data in faces) STBImage.stbi_image_free(data);
            }


        }
    }

    private fun processCubeMapTexture(cubeMapData: Array<ByteBuffer>, width : Int, height : Int) {
        texID = GL30.glGenTextures();
        GL30.glBindTexture(GL30.GL_TEXTURE_CUBE_MAP, texID);
        for(i in 0..cubeMapData.lastIndex) {
            GL30.glTexImage2D(GL30.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i,0,
                    GL30.GL_RGBA, width, height, 0,
                    GL30.GL_RGBA, GL30.GL_UNSIGNED_BYTE,
                    cubeMapData[i]);
        }

        setTexParams(GL30.GL_CLAMP_TO_EDGE, GL30.GL_CLAMP_TO_EDGE, GL30.GL_CLAMP_TO_EDGE, GL30.GL_LINEAR, GL30.GL_LINEAR);
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, 0);
    }


    fun setTexParams(wrapS: Int, wrapT: Int, wrapR: Int, minFilter: Int, magFilter: Int) {
        GL30.glTexParameteri(GL30.GL_TEXTURE_CUBE_MAP, GL30.GL_TEXTURE_WRAP_S, wrapS);
        GL30.glTexParameteri(GL30.GL_TEXTURE_CUBE_MAP, GL30.GL_TEXTURE_WRAP_T, wrapT);
        GL30.glTexParameteri(GL30.GL_TEXTURE_CUBE_MAP, GL30.GL_TEXTURE_WRAP_R, wrapR);
        GL30.glTexParameteri(GL30.GL_TEXTURE_CUBE_MAP, GL30.GL_TEXTURE_MIN_FILTER, minFilter);
        GL30.glTexParameteri(GL30.GL_TEXTURE_CUBE_MAP, GL30.GL_TEXTURE_MAG_FILTER, magFilter);
    }

    fun bind(textureUnit: Int) {
        GL30.glActiveTexture(textureUnit);
        GL30.glBindTexture(GL30.GL_TEXTURE_CUBE_MAP, this.texID);

    }

    fun unbind() {
        GL30.glBindTexture(GL30.GL_TEXTURE_CUBE_MAP,0);
    }

    fun cleanup() {
        unbind()
        if (texID != 0) {
            GL11.glDeleteTextures(texID)
            texID = 0
        }
    }


}