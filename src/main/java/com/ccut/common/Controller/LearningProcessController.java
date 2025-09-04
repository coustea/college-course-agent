package com.ccut.common.Controller;


import com.ccut.common.entity.ApiResult;
import com.ccut.common.entity.LearningProgress;
import com.ccut.common.service.LearningProgressService;
import com.ccut.common.Utils.ApiResultHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/process")
public class LearningProcessController {

    @Autowired
    private LearningProgressService learningProgressService;


    @GetMapping("/student/{studentId}/course/{courseId}")
    public ApiResult<Optional<LearningProgress>> findByStudentIdAndCourseId(
            @PathVariable("studentId") Long studentId,@PathVariable Long courseId){
        Optional<LearningProgress> optional = learningProgressService.findByStudentIdAndCourseId(studentId,courseId);
        if(optional.isPresent()){
            return ApiResultHandler.success("查找成功",optional);
        }
        else {
            return ApiResultHandler.buildApiResult(500,"查找失败",null);
        }
    }

    @GetMapping("student/{id}")
    public ApiResult<List<LearningProgress>> findByStudentId(@PathVariable Long id) {
        try {
            List<LearningProgress> learningProgressList = learningProgressService.findByStudentId(id);
            if (learningProgressList != null && !learningProgressList.isEmpty()) {
                return ApiResultHandler.success("查找成功",learningProgressList);
            }else{
                return ApiResultHandler.buildApiResult(500,"查找失败",null);
            }
        }catch (Exception e){
            return ApiResultHandler.buildApiResult(500,"查找失败",null);
        }
    }
    @PostMapping
    public ApiResult add(@RequestBody LearningProgress learningProgress) {
        try {
            int res = learningProgressService.addLearningProgress(learningProgress);
            if (res == 1){
                return ApiResultHandler.success("添加成功",learningProgress);
            }
            else{
                return ApiResultHandler.buildApiResult(500,"添加失败",null);
            }
        }catch (Exception e){
            return ApiResultHandler.buildApiResult(500,e.getMessage(),null);
        }
    }
    @PutMapping("/{id}")
    public ApiResult update(@RequestBody LearningProgress learningProgress, @PathVariable int id) {
        if (learningProgress.getId() != id) {
            return ApiResultHandler.buildApiResult(400, "请求路径 ID 与请求体 ID 不一致", null);
        }
        int res = learningProgressService.updateLearningProgress(learningProgress);
        if (res == 1){
            return ApiResultHandler.success("更新成功", learningProgress);
        } else {
            return ApiResultHandler.buildApiResult(500, "更新失败", null);
        }
    }


}
