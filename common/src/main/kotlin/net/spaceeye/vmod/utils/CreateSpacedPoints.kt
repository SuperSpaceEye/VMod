package net.spaceeye.vmod.utils

fun createSpacedPoints(centerPoint: Vector3d, up: Vector3d, right: Vector3d, sideSize: Double, numSidePoints: Int): MutableList<MutableList<Vector3d>> {
    val offsets = linspace(-sideSize/2, sideSize/2, numSidePoints)

    val toReturn = mutableListOf<MutableList<Vector3d>>()

    for (xo in offsets) {
        val temp = mutableListOf<Vector3d>()
        for (yo in offsets) {
            temp.add(centerPoint + up * yo + right * xo)
        }
        toReturn.add(temp)
    }

    return toReturn
}