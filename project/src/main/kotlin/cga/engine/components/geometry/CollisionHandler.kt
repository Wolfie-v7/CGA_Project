package cga.engine.components.geometry

import org.joml.Math.abs
import org.joml.Math.min
import org.joml.Vector3f
import java.lang.Float.max

/**
 * Class that handles collisions using the Separating Axis Theorem (SAT)
 *
 * @property collisionEntities List of Objects represented by their Bounding Boxes that are subscribed to collision detection
 */
class CollisionHandler {
    private var pushBackVector = Vector3f()
    var collisionEntities = mutableListOf<CollisionMesh>()
    var overlapDistance = Float.POSITIVE_INFINITY
    var overlapAxis = Vector3f()
    var debugMessage = ""
    var lastDebugMessage = ""

    fun update(dt: Float, t: Float) {
        for(c1 in collisionEntities.indices) {
            collisionEntities[c1].color = Vector3f(1f, 0f, 0f)
            for (c2 in c1 + 1 until collisionEntities.size) {
                //c2.color = Vector3f(1f, 0f, 0f)
                if(getIsInCollision(collisionEntities[c1], collisionEntities[c2])) {
                    collisionEntities[c2].OnCollision(collisionEntities[c1], overlapDistance, overlapAxis, pushBackVector)
                    collisionEntities[c1].OnCollision(collisionEntities[c2], overlapDistance, overlapAxis, pushBackVector)
                }
            }
        }
    }

    /**
     * Determine if two Oriented Bounding Boxes are in collision using the Separating Axis Theorem (SAT)
     * Based on the paper "Separating Axis Theorem for Oriented Bounding Boxes" by Johnny Huynh
     *
     * Written on: 12/16/2008
     *
     * Revised on: 9/15/2009
     *
     * @param c1 First Oriented Bounding Box
     * @param c2 Second Oriented Bounding Box
     *
     * @return whether the two objects are colliding
     *
     */
    private fun getIsInCollision(c1: CollisionMesh, c2: CollisionMesh): Boolean {
        val diff = Vector3f(c2.center).sub(c1.center)
        val T = Vector3f(diff.x, diff.y, -diff.z)
        val Ax = c1.Ax;
        val Ay = c1.Ay
        val Az = c1.Az
        val Bx = c2.Ax
        val By = c2.Ay
        val Bz = c2.Az
        val WA = c1.halfWidth
        val HA = c1.halfHeight
        val DA = c1.halfDepth
        val WB = c2.halfWidth
        val HB = c2.halfHeight
        val DB = c2.halfDepth

        val Rxx = scalarProjectAB(Ax, Bx)
        val Rxy = scalarProjectAB(Ax, By)
        val Rxz = scalarProjectAB(Ax, Bz)

        val Ryx = scalarProjectAB(Ay, Bx)
        val Ryy = scalarProjectAB(Ay, By)
        val Ryz = scalarProjectAB(Ay, Bz)

        val Rzx = scalarProjectAB(Az, Bx)
        val Rzy = scalarProjectAB(Az, By)
        val Rzz = scalarProjectAB(Az, Bz)

        val AxBx = Vector3f(Ax).cross(Bx) // To optimize
        val AxBy = Vector3f(Ax).cross(By) // To optimize
        val AxBz = Vector3f(Ax).cross(Bz) // To optimize

        val AyBx = Vector3f(Ay).cross(Bx) // To optimize
        val AyBy = Vector3f(Ay).cross(By) // To optimize
        val AyBz = Vector3f(Ay).cross(Bz) // To optimize

        val AzBx = Vector3f(Az).cross(Bx) // To optimize
        val AzBy = Vector3f(Az).cross(By) // To optimize
        val AzBz = Vector3f(Az).cross(Bz) // To optimize


        overlapDistance = Float.MAX_VALUE

        //if(Vector3f(pushBackVector).dot(c2.getWorldZAxis()) > 0) pushBackVector.negate()
        val bHasSeparatingAxis =
            // Case 1
            checkAxis(Vector3f(T), Ax, (WA + abs(WB * Rxx) + abs(HB * Rxy) + abs(DB * Rxz)), 1) ||
            // Case 2
            checkAxis(Vector3f(T), Ay, (HA + abs(WB * Ryx) + abs(HB * Ryy) + abs(DB * Ryz)), 2) ||
            // Case 3
            checkAxis(Vector3f(T), Az, (DA + abs(WB * Rzx) + abs(HB * Rzy) + abs(DB * Rzz)), 3) ||
            // Case 4
            checkAxis(Vector3f(T), Bx, (WB + abs(WA * Rxx) + abs(HA * Ryx) + abs(DA * Rzx)), 4) ||
            // Case 5
            checkAxis(Vector3f(T), By, (HB + abs(WA * Rxy) + abs(HA * Ryy) + abs(DA * Rzy)), 5) ||
            // Case 6
            checkAxis(Vector3f(T), Bz, (DB + abs(WA * Rxz) + abs(HA * Ryz) + abs(DA * Rzz)), 6) ||
            // Case 7
            checkAxis(Vector3f(T), AxBx, (abs(HA * Rzx) + abs(DA * Ryx) + abs(HB * Rxz) + abs(DB * Rxy)), 7) ||
            // Case 8
            checkAxis(Vector3f(T), AxBy, (abs(HA * Rzy) + abs(DA * Ryy) + abs(WB * Rxz) + abs(DB * Rxx)), 8) ||
            // Case 9
            checkAxis(Vector3f(T), AxBz, (abs(HA * Rzz) + abs(DA * Ryz) + abs(WB * Rxy) + abs(HB * Rxx)), 9) ||
            // Case 10
            checkAxis(Vector3f(T), AyBx, (abs(WA * Rzx) + abs(DA * Rxx) + abs(HB * Ryz) + abs(DB * Ryy)), 10) ||
            // Case 11
            checkAxis(Vector3f(T), AyBy, (abs(WA * Rzy) + abs(DA * Rxy) + abs(WB * Ryz) + abs(DB * Ryx)), 11) ||
            // Case 12
            checkAxis(Vector3f(T), AyBz, (abs(WA * Rzz) + abs(DA * Rxz) + abs(WB * Ryy) + abs(HB * Ryx)), 12) ||
            // Case 13
            checkAxis(Vector3f(T), AzBx, (abs(WA * Ryx) + abs(HA * Rxx) + abs(HB * Rzz) + abs(DB * Rzy)), 13) ||
            // Case 14
            checkAxis(Vector3f(T), AzBy, (abs(WA * Ryy) + abs(HA * Rxy) + abs(WB * Rzz) + abs(DB * Rzx)), 14) ||
            // Case 15
            checkAxis(Vector3f(T), AzBz, (abs(WA * Ryz) + abs(HA * Rxz) + abs(WB * Rzy) + abs(HB * Rzx)), 15)

        //println("Result: ${bHasSeparatingAxis.not()}")

        pushBackVector =
            if(overlapAxis.x == 0f && overlapAxis.z == 0f) Vector3f(0f, 1f, 0f)
            else Vector3f(
                    if(Vector3f(Az).dot(Bz) < 0) T.x().unaryMinus() else T.x(),
                    0f,
                    if(Vector3f(Az).dot(Bz) > 0) T.z().unaryMinus() else T.z())
                    .normalize();
        /*if(debugMessage != lastDebugMessage) {
            lastDebugMessage = debugMessage
            println(lastDebugMessage)
        }
        debugMessage = ""*/
        return !bHasSeparatingAxis;
    }

    private fun scalarProjectAB(A: Vector3f, B: Vector3f ) : Float {
        //println("${A.dot(B)}")
        return if(B == Vector3f(0f)) Float.NEGATIVE_INFINITY else abs(A.dot(B) )
    }

    private fun checkAxis(T: Vector3f, axis: Vector3f, boundsProjection: Float, case: Int) : Boolean {
        val distanceProjection: Float = scalarProjectAB(T, axis)

        if(distanceProjection == Float.NEGATIVE_INFINITY) return false

        //debugMessage += "$case: ${abs(distanceProjection - boundsProjection)} == "
        //println("$case: $distanceProjection == $boundsProjection")
        if(distanceProjection > boundsProjection) return true

        /*if(
            abs(distanceProjection - boundsProjection) < overlapDistance
            && Vector3f(0f, 1f, 0f).dot(axis) == 0f
        ) pushBackVector = Vector3f(axis);*/
        if(abs(distanceProjection - boundsProjection) < overlapDistance) {
            overlapAxis = axis

        }
        overlapDistance = max(min(abs(distanceProjection - boundsProjection), overlapDistance), 0.05f)
        //println(distanceProjection - boundsProjection)

        //debugMessage += "$overlapDistance == $pushBackVector\n"

        return false;
    }
}