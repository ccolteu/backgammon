package com.example.backgammon.ui.game

data class Euler(val x: Float, val y: Float, val z: Float)

fun topFaceRotation(value: Int): Euler =
  when (value) {
    2 -> Euler(-90f, 0f, 0f)
    3 -> Euler(-90f, -90f, 0f)
    4 -> Euler(-90f, 90f, 0f)
    5 -> Euler(90f, 0f, 0f)
    6 -> Euler(180f, 0f, 0f)
    else -> Euler(0f, 0f, 0f)
  }

fun lookAtTable(yaw: Float): Euler = Euler(x = 24f, y = yaw, z = 0f)

fun settledPose(value: Int, yaw: Float): Euler {
  val top = topFaceRotation(value.coerceIn(1, 6))
  val look = lookAtTable(yaw)
  return Euler(top.x + look.x, top.y + look.y, top.z + look.z)
}

data class Vec3(val x: Float, val y: Float, val z: Float)

fun faceTowardCamera(normal: Vec3, rot: Euler): Float {
  var v = rotateZ(normal, rot.z)
  v = rotateX(v, rot.x)
  v = rotateY(v, rot.y)
  return v.z
}

private fun rotateX(v: Vec3, deg: Float): Vec3 {
  val r = Math.toRadians(deg.toDouble())
  val c = kotlin.math.cos(r).toFloat()
  val s = kotlin.math.sin(r).toFloat()
  return Vec3(v.x, v.y * c - v.z * s, v.y * s + v.z * c)
}

private fun rotateY(v: Vec3, deg: Float): Vec3 {
  val r = Math.toRadians(deg.toDouble())
  val c = kotlin.math.cos(r).toFloat()
  val s = kotlin.math.sin(r).toFloat()
  return Vec3(v.x * c + v.z * s, v.y, -v.x * s + v.z * c)
}

private fun rotateZ(v: Vec3, deg: Float): Vec3 {
  val r = Math.toRadians(deg.toDouble())
  val c = kotlin.math.cos(r).toFloat()
  val s = kotlin.math.sin(r).toFloat()
  return Vec3(v.x * c - v.y * s, v.x * s + v.y * c, v.z)
}

fun rotatePoint(v: Vec3, rot: Euler): Vec3 {
  var p = rotateZ(v, rot.z)
  p = rotateX(p, rot.x)
  p = rotateY(p, rot.y)
  return p
}

data class CubeFaceMesh(val value: Int, val corners: List<Vec3>)

val cubeFaces: List<CubeFaceMesh> =
  listOf(
    CubeFaceMesh(1, listOf(Vec3(-1f, 1f, 1f), Vec3(1f, 1f, 1f), Vec3(1f, 1f, -1f), Vec3(-1f, 1f, -1f))),
    CubeFaceMesh(6, listOf(Vec3(-1f, -1f, -1f), Vec3(1f, -1f, -1f), Vec3(1f, -1f, 1f), Vec3(-1f, -1f, 1f))),
    CubeFaceMesh(2, listOf(Vec3(-1f, -1f, 1f), Vec3(1f, -1f, 1f), Vec3(1f, 1f, 1f), Vec3(-1f, 1f, 1f))),
    CubeFaceMesh(5, listOf(Vec3(1f, -1f, -1f), Vec3(-1f, -1f, -1f), Vec3(-1f, 1f, -1f), Vec3(1f, 1f, -1f))),
    CubeFaceMesh(3, listOf(Vec3(1f, -1f, 1f), Vec3(1f, -1f, -1f), Vec3(1f, 1f, -1f), Vec3(1f, 1f, 1f))),
    CubeFaceMesh(4, listOf(Vec3(-1f, -1f, -1f), Vec3(-1f, -1f, 1f), Vec3(-1f, 1f, 1f), Vec3(-1f, 1f, -1f))),
  )

fun Vec3.length(): Float = kotlin.math.sqrt(x * x + y * y + z * z)

fun Vec3.normalized(): Vec3 {
  val len = length()
  return if (len < 1e-5f) this else Vec3(x / len, y / len, z / len)
}

fun Vec3.dot(other: Vec3): Float = x * other.x + y * other.y + z * other.z

fun faceNormal(corners: List<Vec3>): Vec3 {
  val a = Vec3(corners[1].x - corners[0].x, corners[1].y - corners[0].y, corners[1].z - corners[0].z)
  val b = Vec3(corners[3].x - corners[0].x, corners[3].y - corners[0].y, corners[3].z - corners[0].z)
  return Vec3(a.y * b.z - a.z * b.y, a.z * b.x - a.x * b.z, a.x * b.y - a.y * b.x)
}

fun roundBoxPoint(p: Vec3, radius: Float): Vec3 {
  val inner = 1f - radius
  val q =
    Vec3(
      p.x.coerceIn(-inner, inner),
      p.y.coerceIn(-inner, inner),
      p.z.coerceIn(-inner, inner),
    )
  val dx = p.x - q.x
  val dy = p.y - q.y
  val dz = p.z - q.z
  val len = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
  if (len < 1e-5f) return q
  val s = radius / len
  return Vec3(q.x + dx * s, q.y + dy * s, q.z + dz * s)
}

fun lerp(a: Vec3, b: Vec3, t: Float): Vec3 =
  Vec3(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, a.z + (b.z - a.z) * t)

fun facePoint(face: CubeFaceMesh, u: Float, v: Float): Vec3 {
  val c0 = face.corners[0]
  val c1 = face.corners[1]
  val c2 = face.corners[2]
  val c3 = face.corners[3]
  return lerp(lerp(c0, c1, u), lerp(c3, c2, u), v)
}

fun faceOutline(face: CubeFaceMesh, samples: Int = 14, radius: Float = 0.34f): List<Vec3> {
  val pts = mutableListOf<Vec3>()
  val edges =
    listOf<(Float) -> Vec3>(
      { t -> facePoint(face, t, 0f) },
      { t -> facePoint(face, 1f, t) },
      { t -> facePoint(face, 1f - t, 1f) },
      { t -> facePoint(face, 0f, 1f - t) },
    )
  for (edge in edges) {
    for (i in 0 until samples) {
      pts += roundBoxPoint(edge(i / samples.toFloat()), radius)
    }
  }
  return pts
}
