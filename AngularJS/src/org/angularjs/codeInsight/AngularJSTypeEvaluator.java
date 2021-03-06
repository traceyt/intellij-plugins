package org.angularjs.codeInsight;

import com.intellij.lang.javascript.index.JSNamedElementProxy;
import com.intellij.lang.javascript.psi.*;
import com.intellij.lang.javascript.psi.impl.JSFunctionImpl;
import com.intellij.lang.javascript.psi.resolve.BaseJSSymbolProcessor;
import com.intellij.lang.javascript.psi.resolve.JSTypeEvaluator;
import com.intellij.lang.javascript.psi.types.JSArrayTypeImpl;
import com.intellij.lang.javascript.psi.types.JSCompositeTypeImpl;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.angularjs.index.AngularControllerIndex;
import org.angularjs.index.AngularIndexUtil;
import org.angularjs.lang.psi.AngularJSAsExpression;
import org.angularjs.lang.psi.AngularJSFilterExpression;
import org.angularjs.lang.psi.AngularJSRepeatExpression;

/**
 * @author Dennis.Ushakov
 */
public class AngularJSTypeEvaluator extends JSTypeEvaluator {
  public AngularJSTypeEvaluator(BaseJSSymbolProcessor.EvaluateContext context,
                                BaseJSSymbolProcessor.TypeProcessor processor, boolean ecma) {
    super(context, processor, ecma);
  }

  @Override
  protected boolean addTypeFromElementResolveResult(JSReferenceExpression expression,
                                                    PsiElement parent,
                                                    PsiElement resolveResult,
                                                    boolean wasPrototype,
                                                    boolean hasSomeType) {
    if (resolveResult instanceof JSDefinitionExpression) {
      final PsiElement resolveParent = resolveResult.getParent();
      if (resolveParent instanceof AngularJSAsExpression) {
        final JSNamedElementProxy controller = AngularIndexUtil.resolve(parent.getProject(), AngularControllerIndex.INDEX_ID,
                                                                     resolveParent.getFirstChild().getText());
        final PsiElement element = controller != null ? controller.getElement() : null;
        final PsiElement controllerLiteral = element != null ? element.getParent() : null;
        final JSFunction function = controllerLiteral != null ?
                                    PsiTreeUtil.getNextSiblingOfType(controllerLiteral, JSFunction.class) : null;
        if (function != null) {
          addType(JSFunctionImpl.getReturnTypeInContext(function, expression), resolveResult);
          return true;
        }
      } else if (resolveParent instanceof AngularJSRepeatExpression) {
        if (calculateRepeatParameterType((AngularJSRepeatExpression)resolveParent)) {
          return true;
        }
      }
    }
    return super.addTypeFromElementResolveResult(expression, parent, resolveResult, wasPrototype, hasSomeType);
  }

  private boolean calculateRepeatParameterType(AngularJSRepeatExpression resolveParent) {
    final PsiElement last = findReferenceExpression(resolveParent);
    JSType arrayType = null;
    if (last instanceof JSReferenceExpression) {
      PsiElement resolve = ((JSReferenceExpression)last).resolve();
      resolve = resolve instanceof JSNamedElementProxy ? ((JSNamedElementProxy)resolve).getElement() : resolve;
      resolve = resolve instanceof JSVariable ? ((JSVariable)resolve).getInitializer() : resolve;
      if (resolve instanceof JSExpression) {
        arrayType = evalExprType((JSExpression)resolve);
      }
    } else if (last instanceof JSExpression) {
      arrayType = evalExprType((JSExpression)last);
    }
    final JSType elementType = findElementType(arrayType);
    if (elementType != null) {
      addType(elementType, null);
      return true;
    }
    return false;
  }

  private static JSType findElementType(JSType type) {
    if (type instanceof JSArrayTypeImpl) {
      return ((JSArrayTypeImpl)type).getType();
    }
    if (type instanceof JSCompositeTypeImpl) {
      for (JSType jsType : ((JSCompositeTypeImpl)type).getTypes()) {
        final JSType elementType = findElementType(jsType);
        if (elementType != null) {
          return elementType;
        }
      }
    }
    return null;
  }

  private static PsiElement findReferenceExpression(AngularJSRepeatExpression parent) {
    JSExpression collection = parent.getCollection();
    while (collection instanceof JSBinaryExpression && ((JSBinaryExpression)collection).getROperand() instanceof AngularJSFilterExpression) {
      collection = ((JSBinaryExpression)collection).getLOperand();
    }
    return collection;
  }
}
