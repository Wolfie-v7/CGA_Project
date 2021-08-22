package cga.game

import cga.engine.components.camera.TronCamera
import cga.engine.components.framebuffer.DepthDebug
import cga.engine.components.framebuffer.FrameBuffer
import cga.engine.components.framebuffer.MultSampFrameBuffer
import cga.engine.components.framebuffer.PingPongBuffer
import cga.engine.components.geometry.*
import cga.engine.components.light.DirectionalLight
import cga.engine.components.light.PointLight
import cga.engine.components.light.SpotLight
import cga.engine.components.shader.ShaderProgram
import cga.engine.components.shadows.CascadedShadowMapper
import cga.engine.components.shadows.DirLightShadowMapper
import cga.engine.components.shadows.SpotLightShadowMapper
import cga.engine.components.skybox.CubeMap
import cga.engine.components.skybox.Skybox
import cga.engine.components.terrain.Terrain
import cga.engine.components.terrain.TerrainMaterial
import cga.engine.components.water.WaterSurface
import cga.engine.components.texture.Texture2D
import cga.utility.Vector3Reader
import cga.utility.Vector3Writer
import cga.utility.*
import org.joml.Math.cos
import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL30.GL_CLIP_DISTANCE0
import java.util.*
import kotlin.math.PI


/**
 * Created by Fabian on 16.09.2017.
 */
class Scene(private val window: GameWindow) {


    private var MultiSampled: MultSampFrameBuffer
    private var collisionDebug: Boolean = false
    private var cs: CascadedShadowMapper
    private val playerPositions = mutableListOf<Vector3f>()
    private val collisionHandler = CollisionHandler()
    private var collisionDebugShader: ShaderProgram
    private var collisionBoxes = mutableListOf<CollisionMesh>()
    private var blurFBO: PingPongBuffer
    private var lake: WaterSurface
    private var waterShader: ShaderProgram
    private var blurShader: ShaderProgram
    private var depthMapInstShader: ShaderProgram
    private var instanceShader: ShaderProgram
    private var terrainShader: ShaderProgram
    private var terrain: Terrain
    private val ActorList = mutableListOf<Actor>();
    private var depthMapDebugShader: ShaderProgram
    private var depthDebug: DepthDebug;
    private val mainShader: ShaderProgram;
    private val skyboxShader: ShaderProgram;
    private var postProcessShader: ShaderProgram;
    private var PostProcess : FrameBuffer;
    private var depthMapShader : ShaderProgram;
    private var dirShadowMapper : DirLightShadowMapper;
    private var sLightShadowMappers = mutableListOf<SpotLightShadowMapper>();
    private var treeRen : Renderable?
    private val spawnQueue: Queue<Actor> = LinkedList<Actor>()
    private val despawnQueue: Queue<Actor> = LinkedList<Actor>()

    private var orbitMode: Boolean = false;
    private var Player: Renderable?;
    private var PlayerActor: Actor? = null

    private var Renderables = mutableListOf<Renderable>();
    private var Instances = mutableListOf<RenderableInstance>()
    private var Camera : TronCamera;
    private var CameraArm = Transformable();
    private var DirectionalLight : DirectionalLight;
    private var PointLight : PointLight;
    private var SpotLight : SpotLight;
    private var SkyBox : Skybox? = null;


    private var pointLights = mutableListOf<PointLight>();
    private var spotLights = mutableListOf<SpotLight>();

    // MotorCycle Animation States
    private var jumpState = false; private var speed = 2.0f; private var acceleration = -5.0f; private var fwdSpeed: Float = 0.0f; private var bwdSpeed: Float = 0.0f;
    private var accelerate = false; private var reverse = false; private var brake = false;
    private var leanRight = false; private var leanLeft = false;
    private var leanAngle = 0.000000f; var resetting = false;
    private var c = 0; private var r = 0;

    //Initial Mouse Position
    private var currentMousePos : GameWindow.MousePosition = window.mousePos;


    private val lockList = mutableListOf<GameLock>()

    //scene setup
    init {
        // Shaders Initialization

        terrainShader = ShaderProgram("project/assets/shaders/terrain_vert.glsl", "project/assets/shaders/terrain_frag.glsl")
        mainShader = ShaderProgram("project/assets/shaders/tronAnim_vert.glsl", "project/assets/shaders/tronCascaded_frag.glsl");
        skyboxShader = ShaderProgram("project/assets/shaders/skybox_vert.glsl", "project/assets/shaders/skybox_frag.glsl");
        postProcessShader = ShaderProgram("project/assets/shaders/postprocess_vert.glsl", "project/assets/shaders/postprocess_frag.glsl");
        depthMapShader = ShaderProgram("project/assets/shaders/depthMap_vert.glsl", "project/assets/shaders/depthMap_frag.glsl");
        depthMapInstShader = ShaderProgram("project/assets/shaders/depthMapInst_vert.glsl", "project/assets/shaders/depthMapInst_frag.glsl");
        depthMapDebugShader = ShaderProgram("project/assets/shaders/depthMapDebug_vert.glsl", "project/assets/shaders/depthMapDebug_frag.glsl");
        instanceShader = ShaderProgram("project/assets/shaders/inst_vert.glsl", "project/assets/shaders/instCascaded_frag.glsl")
        waterShader = ShaderProgram("project/assets/shaders/water_vert.glsl","project/assets/shaders/water_frag.glsl")
        blurShader = ShaderProgram("project/assets/shaders/blur_vert.glsl","project/assets/shaders/blur_frag.glsl")
        collisionDebugShader = ShaderProgram("project/assets/shaders/collision_vert.glsl","project/assets/shaders/collision_frag.glsl")

        //initial opengl state
        glClearColor(0.0f, 0.0f, 1.0f, 1.0f); GLError.checkThrow();
        glEnable(GL_CULL_FACE); GLError.checkThrow();
        glFrontFace(GL_CCW); GLError.checkThrow();
        glCullFace(GL_BACK); GLError.checkThrow();
        glEnable(GL_DEPTH_TEST); GLError.checkThrow();
        glDepthFunc(GL_LESS); GLError.checkThrow();
        //glEnable(GL_FRAMEBUFFER_SRGB)

        //===================================================================
        // Terrain
        //===================================================================
        val grass = Material(Texture2D.invoke("project/assets/textures/forest01Mat/forest_d.png",
            genMipMaps = true,
            isSRGB = true
        ),
            Texture2D.invoke("project/assets/textures/grass_emit.png", true),
            Texture2D.invoke("project/assets/textures/grass_emit.png", true),
            100f, Vector2f(512f, 512f));
        grass.normalMap = Texture2D("project/assets/textures/forest01Mat/forest_n.png", true);

        val ground = Material(Texture2D.invoke("project/assets/textures/forest02Mat/grass_d.jpg",
            genMipMaps = true,
            isSRGB = true
        ),
            Texture2D.invoke("project/assets/textures/grass_emit.png", true),
            Texture2D.invoke("project/assets/textures/grass_emit.png", true),
            100f, Vector2f(512f, 512f))
        ground.normalMap = Texture2D("project/assets/textures/forest02Mat/grass_n.jpg", true);

        val wetPebble = Material(Texture2D.invoke("project/assets/textures/TerrainTextures/WetPebblesTextures/Ground_Wet_Pebbles_001_basecolor.jpg",
            genMipMaps = true,
            isSRGB = true
        ),
            Texture2D.invoke("project/assets/textures/grass_emit.png", true),
            Texture2D.invoke("project/assets/textures/TerrainTextures/WetPebblesTextures/Ground_Wet_Pebbles_001_specular.jpg", true),
            100f, Vector2f(512f, 512f))
        wetPebble.normalMap = Texture2D("project/assets/textures/TerrainTextures/WetPebblesTextures/Ground_Wet_Pebbles_001_normal.jpg", true)

        val mountain = Material(Texture2D.invoke("project/assets/textures/TerrainTextures/RockTextures/Rock_042_BaseColor.jpg",
            genMipMaps = true,
            isSRGB = true
        ),
            Texture2D.invoke("project/assets/textures/grass_emit.png", true),
            Texture2D.invoke("project/assets/textures/grass_emit.png", true),
            100f, Vector2f(8f, 8f))
        mountain.normalMap = Texture2D("project/assets/textures/TerrainTextures/RockTextures/Rock_042_Normal.jpg", true)

        val terrainMat = TerrainMaterial(
            blendMap = Texture2D("project/assets/textures/TerrainTextures/blendMapR4.png", true),
            rMaterial = mountain,
            gMaterial = ground,
            bMaterial = wetPebble,
            backgroundMat = grass
        )
        terrain = Terrain(0, 0, terrainMat, "project/assets/textures/Heightmaps/heightmap_f.png");

        lake = WaterSurface(Vector3f(-330f, -1f, -480f), Vector3f(0f), 500f)
        lake.setDistortion(0.1f)
        lake.setTCMul(Vector2f(16f, 16f))
        lake.setSpeed(0.01f)
        lake.setShininess(16f)

        //===================================================================

        var tree = ModelLoader.loadModel("project/assets/models/Trees/TreeA.obj", 0f, 0f, 0f);
        tree?.MeshList?.get(0)?.material = Material(
                Texture2D("project/assets/models/Bark_06_baseColor.jpg", true),
                Texture2D("project/assets/textures/grass_emit.png", true),
                Texture2D("project/assets/models/maple_bark.png", true),
                60f, Vector2f(2f, 16f))

        tree?.MeshList?.get(0)?.material?.normalMap = Texture2D("project/assets/models/Bark_06_normal.jpg", true)
        tree?.MeshList?.get(1)?.bHasTransparency = true;
        //tree?.translateLocal(Vector3f(100f,0f,0f))
        tree?.scaleLocal(Vector3f(2f))
        treeRen = tree
        //tree?.translateLocal(Vector3f(5f, 0f, 0f));
        //tree?.let { println(it.MeshList.size); Renderables.add(it) }
        //tree?.translateLocal(Vector3f(-100f,0f,0f))

        val treesPositions = PositionGenerator.generatePositions(100, Vector3f(-30f, 0f, -50f), Vector3f(50f, 0f, 10f), 10f, 0f, 10f)
        var treeInst = tree?.let { r ->
            treesPositions
                ?.let { RenderableInstance(r, it,100, tree.getWorldModelMatrix()) }
        }
        treeInst?.let { Instances.add(it) }


        val grassObj = ModelLoader.loadModel("project/assets/models/grass.obj", 0f, 0f, 0f);
        grassObj?.MeshList?.forEach { it.bHasTransparency = true; it.material?.diff?.setTexParams(GL30.GL_CLAMP_TO_EDGE, GL30.GL_CLAMP_TO_EDGE, GL30.GL_LINEAR_MIPMAP_LINEAR, GL30.GL_LINEAR_MIPMAP_LINEAR) }
        val grassPositions = PositionGenerator.generatePositions(1000, Vector3f(-70f, 0f, -100f), Vector3f(70f, 0f, 50f), 2f, 0.05f, 2f, terrain)
        val grassInst = grassObj?.let { g ->
            grassPositions?.let { RenderableInstance(g, it, 1000, grassObj.getWorldModelMatrix(), null, true)}

        }
        grassInst?.let { Instances.add(it) }

        val pos = Vector3Reader.read("project/assets/cache/PlayerPositions.txt")
        val tree1obj = ModelLoader.loadModel("project/assets/cache/tree1.obj", 0f, 0f, 0f)
        tree1obj?.MeshList?.forEach { it.bHasTransparency = true; it.material?.diff?.setTexParams(GL30.GL_CLAMP_TO_EDGE, GL30.GL_CLAMP_TO_EDGE, GL30.GL_LINEAR_MIPMAP_LINEAR, GL30.GL_LINEAR_MIPMAP_LINEAR) }
        val tree1Positions = PositionGenerator.generatePositions(500, pos?.get(0) ?: Vector3f(), pos?.get(1) ?: Vector3f(), 5f, 0f, 5f)
        val tree1Inst = tree1obj?.let { t -> tree1Positions?.let { RenderableInstance(t, it, 500, tree1obj.getWorldModelMatrix()) }}
        tree1Inst?.let { Instances.add(it) }

        //===================================================================
        // Rocks
        //===================================================================

        val rock1 = ModelLoader.loadModel("project/assets/models/static/Rock_9/Rock.obj", 0f, 0f, 0f)
        val rock1Pos = PositionGenerator.generatePositions(7, Vector3f(-70f, -0.5f, -70f), Vector3f(20f, 0.5f, -20f),20f, 0f, 20f)
        val rock1Inst = rock1?.let { t -> rock1Pos?.let { RenderableInstance(t, it, 7, rock1.getWorldModelMatrix()) } }
        rock1Inst?.let { Instances.add(it) }

        val rock2 = ModelLoader.loadModel("project/assets/models/static/rock_4/rock.obj", 0f, 0f, 0f)
        val rock2Pos = PositionGenerator.generatePositions(3, Vector3f(100f, 0f, -10f), Vector3f(200f, 0f, 30f),20f, 0f, 20f)
        val rock2Mat = Material(
            Texture2D("project/assets/models/static/rock_4/rock_low_Base_Color.png", true),
            Texture2D("project/assets/models/static/rock_4/rock_low_Metallic.png", true),
            Texture2D("project/assets/models/static/rock_4/rock_low_Metallic.png", true))
        rock2Mat.normalMap = Texture2D("project/assets/models/static/rock_4/rock_low_Normal_DirectX.png", true)
        rock2?.setMaterial(rock2Mat)
        val rock2Inst = rock2?.let { t ->  rock2Pos?.let { RenderableInstance(t, it, 3, rock2.getWorldModelMatrix()) }}
        rock2Inst?.let { Instances.add(it) }

        val rock3 = ModelLoader.loadModel("project/assets/models/static/Rock/Rock_3.obj", 0f, 0f, 0f)
        rock3?.scaleLocal(Vector3f(0.05f))
        val rock3Pos = PositionGenerator.generatePositions(5, Vector3f(-3000f, 0f, -2500f), Vector3f(1000f, 0f, -2000f),50f, 0f, 20f)
        val rock3Mat = Material(
            Texture2D("project/assets/models/static/Rock/Rock_d.jpg", true),
            Texture2D("project/assets/models/static/rock_4/rock_low_Metallic.png", true),
            Texture2D("project/assets/models/static/Rock/Rock_s.jpg", true))
        rock3Mat.normalMap = Texture2D("project/assets/models/static/Rock/Rock_n.jpg", true)
        rock3?.setMaterial(rock3Mat)
        val rock3Inst = rock3?.let { t ->  rock3Pos?.let { RenderableInstance(t, it, 5, rock3.getWorldModelMatrix()) }}
        rock3Inst?.let { Instances.add(it) }

        //===================================================================
        // Player
        //===================================================================

        Player = ModelLoader.loadModel("project/assets/models/OrangeBot_OBJ/mainBot.dae", 0f, 0f, 0f)
        //Player?.setDrawLine(true)
        //println(Player?.animations?.size)

        Player?.let { ModelLoader.loadAnimation("project/assets/models/OrangeBot_OBJ/mainBotWavingAnim.dae", it) }
        //println(Player?.animations?.size)

        //Player?.scaleLocal(Vector3f(4f))


        //===================================================================

        // Add to Mesh list for rendering
        //Player?.let { Renderables.add(it)}

        PlayerActor = Player?.let { Player(this, it) }
        Player?.let { PlayerActor?.setSceneRoot(it) }
        PlayerActor?.let { registerActor(it) }


        //===================================================================

        //===================================================================
        // Puzzle 1
        //===================================================================
        val rabbitKey = ModelLoader.loadModel("project/assets/models/puzzle1/rabbit/RabbitKey.obj", 0f, 0f, 0f)
        val rabbitKeyActor = rabbitKey?.let {
            it.translateLocal(Vector3f(18f, 0f, 5f));
            GameKey(this, it, GameKey.KeyType.RABBIT) }
        rabbitKey?.let { rabbitKeyActor?.setSceneRoot(it) }
        rabbitKeyActor?.let { registerActor(it) }

        val rabbitPlatform = ModelLoader.loadModel("project/assets/models/puzzle1/rabbit/RabbitPlatform.obj", 0f, 0f, 0f)
        val rabbitPlatformActor = rabbitPlatform?.let {
            it.MeshList[1].material?.normalMap = Texture2D("project/assets/models/puzzle1/rabbit/RabbitPlatform_normal.jpg", true)
            it.translateLocal(Vector3f(8f, 0f, 5f));
            GameLock(this, it, GameKey.KeyType.RABBIT)
        }

        rabbitPlatform?.let { rabbitPlatformActor?.setSceneRoot(it) }
        rabbitPlatformActor?.let { registerActor(it); lockList.add(it) }

        val bearKey = ModelLoader.loadModel("project/assets/models/puzzle1/bear/BearKey.obj", 0f, 0f, 0f)
        val bearKeyActor = bearKey?.let {
            it.translateLocal(Vector3f(35f, 0f, 15f));
            GameKey(this, it, GameKey.KeyType.BEAR) }
        bearKey?.let { bearKeyActor?.setSceneRoot(it) }
        bearKeyActor?.let { registerActor(it) }

        val bearPlatform = ModelLoader.loadModel("project/assets/models/puzzle1/bear/BearPlatform.obj", 0f, 0f, 0f)
        val bearPlatformActor = bearPlatform?.let {
            it.MeshList[1].material?.normalMap = Texture2D("project/assets/models/puzzle1/bear/Platform_normal.jpg", true)
            it.translateLocal(Vector3f(25f, 0f, -8f));
            GameLock(this, it, GameKey.KeyType.BEAR)
        }

        bearPlatform?.let { bearPlatformActor?.setSceneRoot(it) }
        bearPlatformActor?.let { registerActor(it); lockList.add(it) }

        val wolfKey = ModelLoader.loadModel("project/assets/models/puzzle1/wolf/WolfKey.obj", 0f, 0f, 0f)
        val wolfKeyActor = wolfKey?.let {
            it.translateLocal(Vector3f(-5f, 0f, -20f));
            GameKey(this, it, GameKey.KeyType.WOLF) }
        wolfKey?.let { wolfKeyActor?.setSceneRoot(it) }
        wolfKeyActor?.let { registerActor(it) }

        val wolfPlatform = ModelLoader.loadModel("project/assets/models/puzzle1/wolf/WolfPlatform.obj", 0f, 0f, 0f)
        val wolfPlatformActor = wolfPlatform?.let {
            //it.MeshList[1].material?.normalMap = Texture2D("project/assets/models/puzzle1/wolf/Platform_normal.jpg", true)
            it.translateLocal(Vector3f(-10f, 0f, 7f));
            GameLock(this, it, GameKey.KeyType.WOLF)
        }

        wolfPlatform?.let { wolfPlatformActor?.setSceneRoot(it) }
        wolfPlatformActor?.let { registerActor(it); lockList.add(it) }



        //===================================================================
        // Camera Initialization
        //===================================================================

        CameraArm = Transformable(Matrix4f(), Player)
        Camera = TronCamera(DegToRad(45.0f), 16.0f / 9.0f, 0.1f, 1000.0f, CameraArm);
        Camera.rotateLocal(DegToRad(-20.0f), 0.0f, 0.0f);
        Camera.translateLocal(Vector3f(0.0f, 0.0f, 7.0f));
        Camera.rotateLocal(DegToRad(5.0f), 0.0f, 0.0f);
        Camera.setRadius(CameraArm.getWorldPosition().sub(Camera.getWorldPosition()).length())


        //===================================================================

        //===================================================================
        // Light
        //===================================================================

        DirectionalLight = DirectionalLight(Vector3f(0.1f), Vector3f(2f), Vector3f(2.0f), Vector3f(1.0f, 1.0f, 0.0f));
        PointLight = PointLight(Vector3f(), Vector3f(0.7f, 0.0f, 0.901f), Vector3f(0.7f, 0.5f, 1.0f), Player);
        PointLight.translateLocal(Vector3f(0.0f, 1.5f, 0.0f));
        SpotLight = SpotLight(
                Vector3f(0.0f, 0.0f, -1.0f),
                cos(DegToRad(20.0f)),
                cos(DegToRad(30.0f)),
                Vector3f(), Vector3f(1.0f), Vector3f(1.0f), Player);
        SpotLight.translateLocal(Vector3f(0.0f, 2.0f, -3.0f));
        SpotLight.rotateLocal(DegToRad(-50.0f), 0.0f, 0.0f);

        PointLight.setAttenuation(1.0f, 0.5f, 0.1f);
        SpotLight.setAttenuation(0.5f, 0.05f, 0.01f);


        //pointLights.add(PointLight);
        spotLights.add(SpotLight);

        for(i in 0..4) {
            val s = SpotLight(Vector3f(0.0f, 0.0f, -10.0f),
                    cos(DegToRad(40.0f)),
                    cos(DegToRad(50.0f)),
                    Vector3f(), Vector3f(1.0f), Vector3f(1.0f));
            val sm = SpotLightShadowMapper(2048, window.windowWidth, window.windowHeight, s);
            s.translateLocal(Vector3f((-1.0 + 2 * Math.random()).times(10).toFloat(), 3.0f, (-1.0 + 2 * Math.random()).times(10).toFloat()));
            s.rotateLocal(DegToRad(-90.0f), 0.0f, 0.0f);
            spotLights.add(s);
            sLightShadowMappers.add(sm);
        }
        for(i in 0..3) {
            val p = PointLight(Vector3f(), Vector3f(0.7f, 0.0f, 0.901f), Vector3f(0.7f, 0.5f, 1.0f));
            when (i) {
                0 -> p.translateLocal(Vector3f(-15f, 3f, 15f));
                1 -> p.translateLocal(Vector3f(15f, 3f, 15f));
                2 -> p.translateLocal(Vector3f(-15f, 3f, -15f));
                3 -> p.translateLocal(Vector3f(15f, 3f, -15f));
            }
            //pointLights.add(p);
        }

        //println(spotLights.size)

        //===================================================================

        //===================================================================
        // SkyBox
        //===================================================================
        SkyBox = Skybox(CubeMap.invoke("project/assets/textures/Skyboxes/puresky/"), Camera);

        //===================================================================

        //===================================================================
        // Post Process FBO
        //===================================================================

        PostProcess = FrameBuffer(window.windowWidth, window.windowHeight);
        postProcessShader.setUniform("ScreenTexture", 0);

        MultiSampled = MultSampFrameBuffer(window.windowWidth, window.windowHeight)

        //===================================================================

        //===================================================================
        // Shadows
        //===================================================================
        dirShadowMapper = DirLightShadowMapper(2048, window.windowWidth, window.windowHeight, DirectionalLight, Camera);
        depthDebug = DepthDebug(window.windowWidth, window.windowHeight);

        // draw as wireframe
        //glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);


        cs = CascadedShadowMapper(Camera, window.windowWidth, window.windowHeight, DirectionalLight)

        //Renderables.add(lake.getMesh())


        //===================================================================
        // PingPong Frame Buffer
        //===================================================================
        blurFBO = PingPongBuffer(window.windowWidth, window.windowHeight)

        val boxVertices = floatArrayOf(
            -1f, -1f,  1f,
             1f, -1f,  1f,
             1f,  1f,  1f,
            -1f,  1f,  1f,
            -1f, -1f, -1f,
             1f, -1f, -1f,
             1f,  1f, -1f,
            -1f,  1f, -1f
        )

        val boxIndices = intArrayOf(
            0, 1, 2,
            0, 2, 3,
            0, 4, 5,
            0, 5, 1,
            0, 3, 7,
            0, 7, 4,
            6, 2, 1,
            6, 1, 5,
            6, 7, 3,
            6, 3, 2,
            6, 5, 4,
            6, 4, 7
        )

        val box = CollisionMesh(boxVertices, boxIndices, arrayOf(VertexAttribute(3, GL_FLOAT, 12, 0)))
        val box2 = CollisionMesh(boxVertices, boxIndices, arrayOf(VertexAttribute(3, GL_FLOAT, 12, 0)))

        box.translateLocal(Vector3f(3f, 1f, 3f))
        box2.translateLocal(Vector3f(3f, 1f, 5.1f))

        //collisionBoxes.add(box2);

        val robotCollision = CollisionMesh(boxVertices, boxIndices, arrayOf(VertexAttribute(3, GL_FLOAT, 12, 0)), Player)


        robotCollision.translateLocal(Vector3f(0f, 1f, 0f))
        collisionBoxes.add(box);


        for(i in 0 until (treeInst?.instances?.size ?: 0)) {
            val treeCollision = CollisionMesh(boxVertices, boxIndices, arrayOf(VertexAttribute(3, GL_FLOAT, 12, 0)))
            treeInst?.instances?.get(i)?.getWorldPosition().let {
                //treeCollision.translateGlobal(it)
                treeCollision.modelMatrix.translate(it)
                treeCollision.translateLocal(Vector3f(0f, 2f, 0f))
            }
            collisionBoxes.add(treeCollision)
        }

        collisionBoxes.add(robotCollision);
        collisionHandler.collisionEntities = collisionBoxes

        //===================================================================



    }



    fun render(dt: Float, t: Float) {

        // First Pass: Render scene to the depth map of each Shadow caster
        depthMapShader.use();
        /*dirShadowMapper.bind(depthMapShader);
        for (obj in Renderables) obj.render(depthMapShader);
        for (actor in ActorList) actor.Render(depthMapShader)
        for(inst in Instances) inst.render(depthMapShader);
        dirShadowMapper.unbind();*/

        /*for(mapper in sLightShadowMappers)
        {
            mapper.bind(depthMapShader);
            for (obj in Renderables) obj.render(depthMapShader);
            //for(inst in Instances) inst.render(depthMapShader);
            mapper.unbind();
        }*/

        cs.render(depthMapShader) {
            for (obj in Renderables) obj.render(depthMapShader);
            for (actor in ActorList) actor.Render(depthMapShader)
            for (inst in Instances) inst.render(depthMapShader);
        }

        /*depthMapInstShader.use()
        dirShadowMapper.bind(depthMapInstShader);
        for(inst in Instances) inst.render(depthMapInstShader);
        dirShadowMapper.unbind();
        for(mapper in sLightShadowMappers)
        {
            mapper.bind(depthMapInstShader);
            for(inst in Instances) inst.render(depthMapInstShader);
            mapper.unbind();
        }*/

        var view_norm_matrix = Camera.getCalculateViewMatrix();

        // Second Pass: Render Scene to Post Process FBO
        //PostProcess.bind(window.windowWidth, window.windowHeight);
        MultiSampled.bind(window.windowWidth, window.windowHeight)
        glEnable(GL_DEPTH_TEST);
        glClearColor(0.5f, 0.6f, 0.7f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT);


        //================================================================================================================================


        //================================================================================================================================

        /**
         * Instance Rendering
         */
        instanceShader.use()
        Camera.bind(instanceShader);
        (instanceShader.setUniform("clipPlane", Vector4f(0f, -1f, 0f, 2f)))

        instanceShader.setUniform("NUM_SHADOWCASTERS",  sLightShadowMappers.size + 1);
        instanceShader.setUniform("shadowMapsCount", sLightShadowMappers.size + 1);

        //dirShadowMapper.configureMatrices(instanceShader, 0);
        //dirShadowMapper.bindDepthTexture(instanceShader, 0);

        cs.configureMatrices(instanceShader)
        cs.bindDepthTextures(instanceShader)
        for(m in sLightShadowMappers)
        {
            m.configureMatrices(instanceShader, sLightShadowMappers.indexOf(m) + 1);
            m.bindDepthTexture(instanceShader, sLightShadowMappers.indexOf(m) + 1);
        }

        instanceShader.setUniform("viewPosition", Camera.getWorldPosition());
        DirectionalLight.bind(instanceShader, "dirLight", Camera.getCalculateViewMatrix());
        for (pl in pointLights) pl.bind(instanceShader, "pointLights[${pointLights.indexOf(pl)}]");
        for (sl in spotLights) sl.bind(instanceShader, "spotLights[${spotLights.indexOf(sl)}]", Camera.getCalculateViewMatrix());

        for(inst in Instances) {
            view_norm_matrix = Camera.getCalculateViewMatrix();
            view_norm_matrix.mul(inst.modelMatrix);
            view_norm_matrix.invert();
            instanceShader.setUniform("view_normal_matrix", view_norm_matrix, true);
            inst.render(instanceShader);
        }



        //================================================================================================================================
        /**
         * Terrain Rendering
         */
        terrainShader.use()
        //glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        Camera.bind(terrainShader);

        terrainShader.setUniform("NUM_SHADOWCASTERS",  sLightShadowMappers.size + 1);
        terrainShader.setUniform("shadowMapsCount", sLightShadowMappers.size + 1);

        //dirShadowMapper.configureMatrices(terrainShader, 0);
        //dirShadowMapper.bindDepthTexture(terrainShader, 0);

        cs.configureMatrices(terrainShader)
        cs.bindDepthTextures(terrainShader)
        /*for(m in sLightShadowMappers)
        {
            m.configureMatrices(terrainShader, sLightShadowMappers.indexOf(m) + 1);
            m.bindDepthTexture(terrainShader, sLightShadowMappers.indexOf(m) + 1);
        }*/

        terrainShader.setUniform("viewPosition", Camera.getWorldPosition());
        DirectionalLight.bind(terrainShader, "dirLight", Camera.getCalculateViewMatrix());
        for (pl in pointLights) pl.bind(terrainShader, "pointLights[${pointLights.indexOf(pl)}]");
        for (sl in spotLights) sl.bind(terrainShader, "spotLights[${spotLights.indexOf(sl)}]", Camera.getCalculateViewMatrix());

        view_norm_matrix = Camera.getCalculateViewMatrix();
        view_norm_matrix.mul(terrain.getModel()?.modelMatrix);
        view_norm_matrix.invert();
        terrainShader.setUniform("view_normal_matrix", view_norm_matrix, true);
        terrainShader.setUniform("model_normal_matrix", terrain.getModel()?.getLocalModelMatrix()!!.invert(), true);
        terrain.render(terrainShader);

        //================================================================================================================================



        /**
         * Main Rendering
         */
        mainShader.use();
        //glPolygonMode(GL_FRONT, GL_FILL);
        Camera.bind(mainShader);

        mainShader.setUniform("NUM_SHADOWCASTERS",  sLightShadowMappers.size + 1);
        mainShader.setUniform("shadowMapsCount", sLightShadowMappers.size + 1);

        //dirShadowMapper.configureMatrices(mainShader, 0);
        //dirShadowMapper.bindDepthTexture(mainShader, 0);

        cs.configureMatrices(mainShader)
        cs.bindDepthTextures(mainShader)

        for(m in sLightShadowMappers)
        {
            m.configureMatrices(mainShader, sLightShadowMappers.indexOf(m) + 1);
            m.bindDepthTexture(mainShader, sLightShadowMappers.indexOf(m) + 1);
        }

        mainShader.setUniform("viewPosition", Camera.getWorldPosition());



        DirectionalLight.bind(mainShader, "dirLight", Camera.getCalculateViewMatrix());
        for (pl in pointLights) pl.bind(mainShader, "pointLights[${pointLights.indexOf(pl)}]");
        for (sl in spotLights) sl.bind(mainShader, "spotLights[${spotLights.indexOf(sl)}]", Camera.getCalculateViewMatrix());



        // change motorcycle and point light colors over time
        /*var r = max(sin(t), 0f);
        var b = max(cos(t), 0f);
        var rgbLight = Vector3f(r, 0.5f - r, b);
        PointLight.diffuse = rgbLight; PointLight.specular = rgbLight;
        Player?.MeshList?.forEach { it.material!!.emissionColor = rgbLight };*/


        for (renderable in Renderables)
        {
            view_norm_matrix = Camera.getCalculateViewMatrix();
            view_norm_matrix.mul(renderable.modelMatrix);
            view_norm_matrix.invert();
            mainShader.setUniform("view_normal_matrix", view_norm_matrix, true);
            mainShader.setUniform("model_normal_matrix", renderable.getLocalModelMatrix().invert(), true);

            renderable.render(mainShader);
        }

        for (actor in ActorList) {
            view_norm_matrix = Camera.getCalculateViewMatrix();
            view_norm_matrix.mul(actor.getModelMatrix());
            view_norm_matrix.invert();
            mainShader.setUniform("view_normal_matrix", view_norm_matrix, true);
            mainShader.setUniform("model_normal_matrix", actor.getModelMatrix().invert(), true);
            actor.Render(mainShader)
        }


        // Collision Debug
        if(collisionDebug) {
            collisionDebugShader.use()
            for(c in collisionBoxes) {
                c.render(collisionDebugShader, Camera)
            }
        }




        // Skybox rendering
        skyboxShader.use();

        glDepthFunc(GL_LEQUAL);
        SkyBox?.render(skyboxShader);
        glDepthFunc(GL_LESS);

        // Third pass: render scene to water textures (reflection and refraction)
        //=======================================================================
        glEnable(GL_CLIP_DISTANCE0)
        lake.bindReflecBuffer();
        val dis = 2 * (Camera.getWorldPosition().y() - lake.getHeight())
        Camera.modelMatrix.translate(Vector3f(0f, -dis, 0f))
        Camera.invertPitch(1.0)

        /**
         * Terrain Rendering
         */
        terrainShader.use()
        //glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        Camera.bind(terrainShader);
        (terrainShader.setUniform("clipPlane", Vector4f(0f, 1f, 0f, -lake.getHeight() - 5f)))

        terrainShader.setUniform("NUM_SHADOWCASTERS",  sLightShadowMappers.size + 1);
        terrainShader.setUniform("shadowMapsCount", sLightShadowMappers.size + 1);

        //dirShadowMapper.configureMatrices(terrainShader, 0);
        //dirShadowMapper.bindDepthTexture(terrainShader, 0);

        cs.configureMatrices(terrainShader)
        cs.bindDepthTextures(terrainShader)
        for(m in sLightShadowMappers)
        {
            m.configureMatrices(terrainShader, sLightShadowMappers.indexOf(m) + 1);
            m.bindDepthTexture(terrainShader, sLightShadowMappers.indexOf(m) + 1);
        }

        terrainShader.setUniform("viewPosition", Camera.getWorldPosition());
        DirectionalLight.bind(terrainShader, "dirLight", Camera.getCalculateViewMatrix());
        for (pl in pointLights) pl.bind(terrainShader, "pointLights[${pointLights.indexOf(pl)}]");
        for (sl in spotLights) sl.bind(terrainShader, "spotLights[${spotLights.indexOf(sl)}]", Camera.getCalculateViewMatrix());

        view_norm_matrix.mul(terrain.getModel()?.modelMatrix);
        view_norm_matrix.invert();
        terrainShader.setUniform("view_normal_matrix", view_norm_matrix, true);
        terrainShader.setUniform("model_normal_matrix", terrain.getModel()?.getLocalModelMatrix()!!.invert(), true);
        terrain.render(terrainShader);

        //================================================================================================================================

        /**
         * Instance Rendering
         */
        instanceShader.use()
        Camera.bind(instanceShader);
        (instanceShader.setUniform("clipPlane", Vector4f(0f, 1f, 0f, -lake.getHeight() + 0.1f)))

        instanceShader.setUniform("NUM_SHADOWCASTERS",  sLightShadowMappers.size + 1);
        instanceShader.setUniform("shadowMapsCount", sLightShadowMappers.size + 1);

        //dirShadowMapper.configureMatrices(instanceShader, 0);
        //dirShadowMapper.bindDepthTexture(instanceShader, 0);

        cs.configureMatrices(instanceShader)
        cs.bindDepthTextures(instanceShader)
        for(m in sLightShadowMappers)
        {
            m.configureMatrices(instanceShader, sLightShadowMappers.indexOf(m) + 1);
            m.bindDepthTexture(instanceShader, sLightShadowMappers.indexOf(m) + 1);
        }

        instanceShader.setUniform("viewPosition", Camera.getWorldPosition());

        for(inst in Instances) {
            view_norm_matrix = Camera.getCalculateViewMatrix();
            view_norm_matrix.mul(inst.modelMatrix);
            view_norm_matrix.invert();
            instanceShader.setUniform("view_normal_matrix", view_norm_matrix, true);
            inst.render(instanceShader);
        }

        DirectionalLight.bind(instanceShader, "dirLight", Camera.getCalculateViewMatrix());
        for (pl in pointLights) pl.bind(instanceShader, "pointLights[${pointLights.indexOf(pl)}]");
        for (sl in spotLights) sl.bind(instanceShader, "spotLights[${spotLights.indexOf(sl)}]", Camera.getCalculateViewMatrix());

        //================================================================================================================================

        /**
         * Main Rendering
         */
        mainShader.use();
        //glPolygonMode(GL_FRONT, GL_FILL);
        Camera.bind(mainShader);
        (mainShader.setUniform("clipPlane", Vector4f(0f, 1f, 0f, -lake.getHeight() + 0.1f)))

        mainShader.setUniform("NUM_SHADOWCASTERS",  sLightShadowMappers.size + 1);
        mainShader.setUniform("shadowMapsCount", sLightShadowMappers.size + 1);

        //dirShadowMapper.configureMatrices(mainShader, 0);
        //dirShadowMapper.bindDepthTexture(mainShader, 0);

        cs.configureMatrices(mainShader)
        cs.bindDepthTextures(mainShader)
        for(m in sLightShadowMappers)
        {
            m.configureMatrices(mainShader, sLightShadowMappers.indexOf(m) + 1);
            m.bindDepthTexture(mainShader, sLightShadowMappers.indexOf(m) + 1);
        }

        mainShader.setUniform("viewPosition", Camera.getWorldPosition());

        DirectionalLight.bind(mainShader, "dirLight", Camera.getCalculateViewMatrix());
        for (pl in pointLights) pl.bind(mainShader, "pointLights[${pointLights.indexOf(pl)}]");
        for (sl in spotLights) sl.bind(mainShader, "spotLights[${spotLights.indexOf(sl)}]", Camera.getCalculateViewMatrix());

        // change motorcycle and point light colors over time
        /*PointLight.diffuse = rgbLight; PointLight.specular = rgbLight;
        Player?.MeshList?.forEach { it.material!!.emissionColor = rgbLight };*/


        for (renderable in Renderables)
        {
            view_norm_matrix = Camera.getCalculateViewMatrix();
            view_norm_matrix.mul(renderable.modelMatrix);
            view_norm_matrix.invert();
            mainShader.setUniform("view_normal_matrix", view_norm_matrix, true);
            mainShader.setUniform("model_normal_matrix", renderable.getLocalModelMatrix().invert(), true);

            renderable.render(mainShader);
        }

        for (actor in ActorList) {
            view_norm_matrix = Camera.getCalculateViewMatrix();
            view_norm_matrix.mul(actor.getModelMatrix());
            view_norm_matrix.invert();
            mainShader.setUniform("view_normal_matrix", view_norm_matrix, true);
            mainShader.setUniform("model_normal_matrix", actor.getModelMatrix().invert(), true);
            actor.Render(mainShader)
        }

        skyboxShader.use()
        glDepthFunc(GL_LEQUAL);
        (skyboxShader.setUniform("clipPlane", Vector4f(0f, 1f, 0f, -lake.getHeight() + 0.1f)))
        SkyBox?.render(skyboxShader);
        glDepthFunc(GL_LESS);

        Camera.invertPitch(-1.0)
        Camera.modelMatrix.translate(Vector3f(0f, dis, 0f))

        lake.unbindReflecBuffer(window.windowWidth, window.windowHeight)

        //============================================================================================================
        lake.bindRefracBuffer();

        /**
         * Terrain Rendering
         */
        terrainShader.use()
        //glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        Camera.bind(terrainShader);
        (terrainShader.setUniform("clipPlane", Vector4f(0f, -1f, 0f, lake.getHeight() + 3f)))

        terrainShader.setUniform("NUM_SHADOWCASTERS",  sLightShadowMappers.size + 1);
        terrainShader.setUniform("shadowMapsCount", sLightShadowMappers.size + 1);

        //dirShadowMapper.configureMatrices(terrainShader, 0);
        //dirShadowMapper.bindDepthTexture(terrainShader, 0);

        cs.configureMatrices(terrainShader)
        cs.bindDepthTextures(terrainShader)
        for(m in sLightShadowMappers)
        {
            m.configureMatrices(terrainShader, sLightShadowMappers.indexOf(m) + 1);
            m.bindDepthTexture(terrainShader, sLightShadowMappers.indexOf(m) + 1);
        }

        terrainShader.setUniform("viewPosition", Camera.getWorldPosition());
        DirectionalLight.bind(terrainShader, "dirLight", Camera.getCalculateViewMatrix());
        for (pl in pointLights) pl.bind(terrainShader, "pointLights[${pointLights.indexOf(pl)}]");
        for (sl in spotLights) sl.bind(terrainShader, "spotLights[${spotLights.indexOf(sl)}]", Camera.getCalculateViewMatrix());

        view_norm_matrix.mul(terrain.getModel()?.modelMatrix);
        view_norm_matrix.invert();
        terrainShader.setUniform("view_normal_matrix", view_norm_matrix, true);
        terrainShader.setUniform("model_normal_matrix", terrain.getModel()?.getLocalModelMatrix()!!.invert(), true);
        terrain.render(terrainShader);

        //================================================================================================================================

        /**
         * Instance Rendering
         */
        instanceShader.use()
        Camera.bind(instanceShader);
        (instanceShader.setUniform("clipPlane", Vector4f(0f, -1f, 0f, lake.getHeight())))

        instanceShader.setUniform("NUM_SHADOWCASTERS",  sLightShadowMappers.size + 1);
        instanceShader.setUniform("shadowMapsCount", sLightShadowMappers.size + 1);

        //dirShadowMapper.configureMatrices(instanceShader, 0);
        //dirShadowMapper.bindDepthTexture(instanceShader, 0);

        cs.configureMatrices(instanceShader)
        cs.bindDepthTextures(instanceShader)
        for(m in sLightShadowMappers)
        {
            m.configureMatrices(instanceShader, sLightShadowMappers.indexOf(m) + 1);
            m.bindDepthTexture(instanceShader, sLightShadowMappers.indexOf(m) + 1);
        }

        instanceShader.setUniform("viewPosition", Camera.getWorldPosition());

        for(inst in Instances) {
            view_norm_matrix = Camera.getCalculateViewMatrix();
            view_norm_matrix.mul(inst.modelMatrix);
            view_norm_matrix.invert();
            instanceShader.setUniform("view_normal_matrix", view_norm_matrix, true);
            inst.render(instanceShader);
        }

        DirectionalLight.bind(instanceShader, "dirLight", Camera.getCalculateViewMatrix());
        for (pl in pointLights) pl.bind(instanceShader, "pointLights[${pointLights.indexOf(pl)}]");
        for (sl in spotLights) sl.bind(instanceShader, "spotLights[${spotLights.indexOf(sl)}]", Camera.getCalculateViewMatrix());

        //================================================================================================================================

        /**
         * Main Rendering
         */
        mainShader.use();
        //glPolygonMode(GL_FRONT, GL_FILL);
        Camera.bind(mainShader);
        (mainShader.setUniform("clipPlane", Vector4f(0f, -1f, 0f, lake.getHeight())))

        mainShader.setUniform("NUM_SHADOWCASTERS",  sLightShadowMappers.size + 1);
        mainShader.setUniform("shadowMapsCount", sLightShadowMappers.size + 1);

        //dirShadowMapper.configureMatrices(mainShader, 0);
        //dirShadowMapper.bindDepthTexture(mainShader, 0);

        cs.configureMatrices(mainShader)
        cs.bindDepthTextures(mainShader)
        for(m in sLightShadowMappers)
        {
            m.configureMatrices(mainShader, sLightShadowMappers.indexOf(m) + 1);
            m.bindDepthTexture(mainShader, sLightShadowMappers.indexOf(m) + 1);
        }

        mainShader.setUniform("viewPosition", Camera.getWorldPosition());



        DirectionalLight.bind(mainShader, "dirLight", Camera.getCalculateViewMatrix());
        for (pl in pointLights) pl.bind(mainShader, "pointLights[${pointLights.indexOf(pl)}]");
        for (sl in spotLights) sl.bind(mainShader, "spotLights[${spotLights.indexOf(sl)}]", Camera.getCalculateViewMatrix());



        // change motorcycle and point light colors over time
        /*PointLight.diffuse = rgbLight; PointLight.specular = rgbLight;
        Player?.MeshList?.forEach { it.material!!.emissionColor = rgbLight };*/


        for (renderable in Renderables)
        {
            view_norm_matrix = Camera.getCalculateViewMatrix();
            view_norm_matrix.mul(renderable.modelMatrix);
            view_norm_matrix.invert();
            mainShader.setUniform("view_normal_matrix", view_norm_matrix, true);
            mainShader.setUniform("model_normal_matrix", renderable.getLocalModelMatrix().invert(), true);

            renderable.render(mainShader);
        }

        for (actor in ActorList) {
            view_norm_matrix = Camera.getCalculateViewMatrix();
            view_norm_matrix.mul(actor.getModelMatrix());
            view_norm_matrix.invert();
            mainShader.setUniform("view_normal_matrix", view_norm_matrix, true);
            mainShader.setUniform("model_normal_matrix", actor.getModelMatrix().invert(), true);
            actor.Render(mainShader)
        }

        skyboxShader.use()
        glDepthFunc(GL_LEQUAL);
        (skyboxShader.setUniform("clipPlane", Vector4f(0f, -1f, 0f, lake.getHeight())))
        SkyBox?.render(skyboxShader);
        glDepthFunc(GL_LESS);

        lake.unbindRefracBuffer(window.windowWidth, window.windowHeight)


        // Render water
        //PostProcess.bind(window.windowWidth, window.windowHeight)
        MultiSampled.bind(window.windowWidth, window.windowHeight)
        glDisable(GL_CLIP_DISTANCE0)
        waterShader.use()
        Camera.bind(waterShader)
        lake.bindLight(waterShader, DirectionalLight);
        lake.bindTextures(waterShader);
        lake.bindDistortionFactor(waterShader, dt);
        lake.bindCameraPosition(waterShader, Camera.getWorldPosition());
        lake.renderNoMat(waterShader)


        // Final Pass: Render to Screen
        //PostProcess.unbind(window.windowWidth, window.windowHeight);
        MultiSampled.blit(PostProcess)
        MultiSampled.unbind(window.windowWidth, window.windowHeight)


        glDisable(GL_DEPTH_TEST);
        glClearColor(1.0f, 1.0f, 1.0f, 1.0f)
        glClear(GL_COLOR_BUFFER_BIT);

        // Apply bloom
        blurFBO.render(blurShader, PostProcess.getTexture1(), 10)

        // Render to screen
        PostProcess.render(postProcessShader, blurFBO.getBlurTexture());


        if(window.getKeyState(GLFW.GLFW_KEY_P))
            renderDepthMap(0)
        if(window.getKeyState(GLFW.GLFW_KEY_O))
            renderDepthMap(1)
        if(window.getKeyState(GLFW.GLFW_KEY_I))
            renderDepthMap(2)
        if(window.getKeyState(GLFW.GLFW_KEY_U))
            renderDepthMap(3)

        when {
            window.getKeyState(GLFW.GLFW_KEY_L) -> postProcessShader.setUniform("negative", true)
            window.getKeyState(GLFW.GLFW_KEY_G) -> postProcessShader.setUniform("grayscale", true)
            else -> {
                postProcessShader.setUniform("negative", false)
                postProcessShader.setUniform("grayscale", false)
            }
        }


    }
    private fun renderDepthMap(index: Int) {
        depthMapDebugShader.use();
        depthDebug.bind(800, 800);
        depthDebug.setTex(cs.shadowMappersList[index].getDepthMap());
        depthDebug.unbind();
        glDisable(GL_DEPTH_TEST);
        depthDebug.render(depthMapDebugShader, cs.shadowMappersList[index].getDepthMap())
    }

    fun update(dt: Float, t: Float) {

        // FIRST THING TO DO: Handle Spawning and Despawning
        while (spawnQueue.isNotEmpty()) ActorList.add(spawnQueue.poll())
        while (despawnQueue.isNotEmpty()) ActorList.remove(despawnQueue.poll())

        Camera.update(dt, t, terrain)
        collisionHandler.update(dt, t)
        Renderables.forEach {it.update(dt, t)}
        ActorList.forEach {
            if (it is Player) it.Update(dt, t)
            else it.Update(dt, t)
        }
        Instances.forEach { it.update(dt, t) }

        /*if (accelerate) Player?.playAnimation(0, 4f)
        else if(window.getKeyState(GLFW.GLFW_KEY_X)) Player?.playAnimation(1)
        else Player?.stopAnimation()*/


        /**
         * Input Mapping
         *
         * W - accelerate
         * S - reverse / brake
         * A - lean left
         * D - lean right
         * Space - jump
         *
         * Mouse Right Click - Camera Orbit Mode
         */
        //accelerate = window.getKeyState(GLFW.GLFW_KEY_W);
        /*if(window.getKeyState(GLFW.GLFW_KEY_S))
        {
            if(!brake && !reverse)
            {
                if(fwdSpeed != 0.0f) brake = true;
                else reverse = true;
            }
        }
        else
        {
            brake = false; reverse = false;
        }
        leanLeft = window.getKeyState(GLFW.GLFW_KEY_A);
        leanRight = window.getKeyState(GLFW.GLFW_KEY_D);
        if(window.getKeyState(GLFW.GLFW_KEY_SPACE)) jumpState = true;*/
        currentMousePos = window.mousePos; // store current mouse Position every frame


        /**
         * Handles acceleration
         * Cannot accelerate and reverse at the same time
         * can only turn if the forward speed is non-zero
         *  Maximum speed is 1500 unit/dt
         */
        if(accelerate)
        {
            if(!reverse)
            {
                brake = false;
                if (fwdSpeed > -300.0 * dt) fwdSpeed += acceleration * dt;
                Player?.translateLocal(Vector3f(0.0f, 0.0f, dt * fwdSpeed));
                CameraArm.translateLocal(Vector3f(0.0f, 0.0f, dt * fwdSpeed));
                if (leanLeft || leanRight) {
                    Player?.rotateAroundPoint(0.0f, DegToRad(leanAngle * 0.5f), 0.0f, Player!!.getPosition());
                    CameraArm.rotateAroundPoint(0.0f, DegToRad(leanAngle * 0.5f), 0.0f, CameraArm.getPosition())
                }
            }
        }
        else
        {
            if(fwdSpeed < 0.0f) fwdSpeed -= (acceleration / 2.0f) * dt; if(fwdSpeed > 0.0f) fwdSpeed = 0.0f;
            Player?.translateLocal(Vector3f(0.0f, 0.0f, dt * fwdSpeed));
            CameraArm.translateLocal(Vector3f(0.0f, 0.0f, dt * fwdSpeed));
            if ((leanLeft || leanRight) && fwdSpeed < -40.0f * dt) {
                Player?.rotateAroundPoint(0.0f, DegToRad(leanAngle * 0.5f), 0.0f, Player!!.getPosition());
                CameraArm.rotateAroundPoint(0.0f, DegToRad(leanAngle * 0.5f), 0.0f, CameraArm.getPosition())
            }
        }


        /**
         * Handles reverse movement
         * cannot accelerate and reverse at the same time
         * reversing while the motorcycle is moving forward results in braking
         */
        if(reverse)
        {
            if(fwdSpeed < 0.0f) { brake = true; reverse = false; }
            else{
                brake = false;
                if(!accelerate)
                {
                    if (bwdSpeed < 50.0 * dt) bwdSpeed -= acceleration * dt;
                    Player?.translateLocal(Vector3f(0.0f, 0.0f, dt * bwdSpeed));
                    CameraArm.translateLocal(Vector3f(0.0f, 0.0f, dt * bwdSpeed));
                    if (leanLeft || leanRight) {
                        Player?.rotateAroundPoint(0.0f, DegToRad(leanAngle * 0.5f), 0.0f, Player!!.getPosition());
                        CameraArm.rotateAroundPoint(0.0f, DegToRad(leanAngle * 0.5f), 0.0f, CameraArm.getPosition())
                    }
                }
            }
        }

        /**
         * Handles braking
         * braking is only true if reverse is true and forward speed is non-zero
         */
        if(brake)
        {
            if(!accelerate)
            {
                if(fwdSpeed < 0.0f) fwdSpeed -= acceleration * dt; if(fwdSpeed > 0.0f) fwdSpeed = 0.0f;
                Player?.translateLocal(Vector3f(0.0f, 0.0f, dt * fwdSpeed));
                CameraArm.translateLocal(Vector3f(0.0f, 0.0f, dt * fwdSpeed));
            }
        }


        // Set if the motorcycle should be resetting its lean rotation
        resetting = (leanAngle != 0.0000000f && !leanRight && !leanLeft) // Not leaning but the angle is not reset
                || (leanRight && leanLeft) // if both lean buttons are pressed
                || (leanLeft && leanAngle < 0.0f) // if lean left button is pressed but the motorcycle is leaning right
                || (leanRight && leanAngle > 0.0f); // if lean right button is pressed but the motorcycle is leaning left

        // lean left
        if(!leanRight)
        {
            if(leanLeft && leanAngle in 0.0f..1.0f) {
                leanRight = false;
                if (leanAngle < 1.0f) leanAngle += dt * 2.0f; leanAngle = leanAngle.coerceIn(0.0000f, 1.0000f);
                //println(leanAngle)
                if(leanAngle > 0.0f && leanAngle < 1.0f) {
                    //Player?.rotateLocal(0.0f, 0.0f, DegToRad(leanAngle));

                }
                //if(leanAngle <= 0.0f) MotorCycle?.rotateLocal(0.0f, 0.0f, DegToRad(-leanAngle));
            }
        }

        // lean right
        if(!leanLeft)
        {
            if(leanRight && leanAngle in -1.0f..0.0f) {
                leanLeft = false;
                if (leanAngle > -1.0f) leanAngle -= dt * 2.000000f; leanAngle = leanAngle.coerceIn(-1.0000f, 0.0000f);
                //println(leanAngle)
                if(leanAngle > -1.0f && leanAngle <= 0.0f) {
                    //Player?.rotateLocal(0.0f, 0.0f, DegToRad(leanAngle));
                }
                //if(leanAngle >= 0.0f) MotorCycle?.rotateLocal(0.0f, 0.0f, DegToRad(-leanAngle));
            }
        }

        // lean reset
        if(resetting)
        {

            if (leanAngle > 0.0f) {
                //Player?.rotateLocal(0.0f, 0.0f, DegToRad(-leanAngle));
                leanAngle -= dt * 2.0f; leanAngle = leanAngle.coerceIn(0.0000f, 1.0000f);
            }

            if (leanAngle < 0.0f) {
                //Player?.rotateLocal(0.0f, 0.0f, DegToRad(-leanAngle));
                leanAngle += dt * 2.0f; leanAngle = leanAngle.coerceIn(-1.0000f, 0.0000f);
            }


        }

        //Jump
        if(jumpState)
        {
            var maxHeight = 2.5f;


            Player?.translateLocal(Vector3f(0.0f, dt * speed, 0.0f));
            var currentHeight: Float = Player!!.getPosition().y;
            //println(currentHeight);
            if(currentHeight >= maxHeight)
            {
                speed *= -1.0f
            }

            if(currentHeight <= 0.0f) { jumpState = false; speed *= -1.0f;}
        }

        //println("${CameraArm.getPosition()} == ${MotorCycle?.getPosition()}");
        //println(MotorCycle?.getWorldPosition())
        //println(Camera.getCalculateViewMatrix())


    }

    fun onKey(key: Int, scancode: Int, action: Int, mode: Int) {

        if (key == GLFW.GLFW_KEY_C && action == GLFW.GLFW_PRESS) collisionDebug = !collisionDebug

        PlayerActor?.OnKey(key, scancode, action, mode)
        // Save Current Player Position
        /*if (key == GLFW.GLFW_KEY_G && action == GLFW.GLFW_PRESS) {
            Vector3Writer.save(Player?.getWorldPosition() ?: Vector3f(-999f))
        }

        if (key == GLFW.GLFW_KEY_F && action == GLFW.GLFW_PRESS) Vector3Writer.write("project/assets/cache/PlayerPositions.txt")*/
        //if (key == GLFW.GLFW_KEY_R && action == GLFW.GLFW_PRESS) cs.calculateBoundingBox(Camera.getCalculateProjectionMatrix().mul(Camera.getCalculateViewMatrix()))
    }

    fun onMouseButton(button: Int, action: Int, mode: Int) {
        for (actor in ActorList) actor.OnMouseButton(button, action, mode)
        if(button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) { Camera.zoomIN(action == GLFW.GLFW_PRESS) ; }
    }

    fun onMouseMove(xpos: Double, ypos: Double) {
        //if(orbitMode)
            //Camera.rotateAroundPoint(0.0f, DegToRad((currentMousePos.xpos - xpos).toFloat() * 0.03f), 0.0f, Vector3f(0.0f));
        CameraArm.rotateLocal(0f, DegToRad((currentMousePos.xpos - xpos).toFloat() * 0.03f), 0.0f)
        //if(orbitMode)
            Camera.rotateAroundPoint(DegToRad((currentMousePos.ypos - ypos).toFloat() * 0.03f), 0.0f, 0.0f, Vector3f(0.0f));
        //Camera.rotate(DegToRad((currentMousePos.ypos - ypos).toFloat() * 0.03f), 0.0f, 0.0f)

    }

    fun cleanup() {
        PostProcess.cleanup();
        terrain.cleanup();
        Renderables.forEach { renderable -> renderable.MeshList.forEach { it.cleanup() } };
        postProcessShader.cleanup();
        depthDebug.cleanup();
        dirShadowMapper.cleanup();
        sLightShadowMappers.forEach { it.cleanup(); }
        SkyBox?.cleanup();
    }

    private fun DegToRad(AngleInDeg: Float) = (AngleInDeg * PI / 180).toFloat();
    private fun RadToDeg(AngleInRad: Float) = (AngleInRad * 180 / PI).toFloat();
    fun registerActor(actor: Actor) {
        ActorList.add(actor);
    }

    fun removeActor(actor: Actor) {
        if (ActorList.contains(actor)) {
            despawnQueue.add(actor)
        }

    }

    fun BeginPlay() {
        ActorList.forEach { it.BeginPlay()}
    }

    fun contains(actor: Actor): Boolean {
        return ActorList.contains(actor);
    }

    fun getWorldTerrain() = terrain
    fun getWindow() = window
    fun getKeyState(key: Int) = window.getKeyState(key)

    fun spawnActor(actor: Actor, location: Vector3f, rotation: Vector3f) : Actor{
        val actorInst = Actor(actor.world, actor.Mesh, actor.count, actor.bIsInstanced)
        if (actorInst.Mesh is Renderable) {
            (actorInst.Mesh as Renderable).rotateLocal(rotation.x(), rotation.y(), rotation.z())
            (actorInst.Mesh as Renderable).translateLocal(location)
        }
        spawnQueue.add(actorInst)
        return actorInst
    }

    private fun loadLevel() {}

    private fun updateLoadingScreen(percentage: Float) {}

    fun getPlayer(): Player? {
        return PlayerActor as Player?
    }

    fun getLock(type: GameKey.KeyType): GameLock? {
        return lockList.find { it.type == type }
    }

}
