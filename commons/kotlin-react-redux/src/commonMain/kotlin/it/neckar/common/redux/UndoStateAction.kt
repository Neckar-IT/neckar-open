/*
 * Copyright (C) 2013-2026 Neckar IT GmbH, Mössingen, Germany
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library. Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules, regardless of
 * the license terms of these independent modules, and to copy and distribute
 * the resulting combined work under terms of your choice, provided that every
 * copy of the combined work is accompanied by a complete copy of the source
 * code of this library.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package it.neckar.common.redux

/**
 * Redux Actions that rewrite the [UndoState] itself instead of the state it wraps.
 *
 * The reducer therefore never reads [type] for these actions - an implementation owns the whole history
 * bookkeeping in its `updateState` and decides for itself what happens to [UndoState.previous] and
 * [UndoState.future].
 *
 * That is also why the implementations can be singletons over `Any`: they never look at the wrapped state,
 * so the reducer casts them to whatever state type the store holds.
 */
interface UndoStateAction<S> : StateAction<UndoState<S>>

