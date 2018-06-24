package act.controller.captcha.render.img;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2018 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.github.bingoohuang.patchca.filter.library.CurvesImageOp;
import org.osgl.util.Img;

import java.awt.image.BufferedImage;

public class CurvesFilter extends Img.Filter {
    @Override
    protected BufferedImage run() {
        CurvesImageOp op = new CurvesImageOp();
        target = op.filter(target, null);
        return target;
    }
}
