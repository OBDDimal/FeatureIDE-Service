package de.featureide.service.helpclasses

import de.ovgu.featureide.fm.core.base.IFeature
import de.ovgu.featureide.fm.core.base.IFeatureStructure

class ChildFeature (val feature: IFeature, val commonality: Double, val featureStructure: IFeatureStructure, val childrenSubtree: Int, val constraintSubtree: Int, val level: Int) {


}